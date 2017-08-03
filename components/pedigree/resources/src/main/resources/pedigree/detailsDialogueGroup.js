/**
 * DetailsDialogueGroup allows to create one or more instances of DetailsDialogue for some term.
 *
 * @class DetailsDialogueGroup
 * @constructor
 */
// TODO: Move and rename.
define([
        "pedigree/detailsDialogue"
    ], function(
        DetailsDialogue
    ){
    var DetailsDialogueGroup = Class.create({
        initialize: function (dataName, options) {
            this._dataName = dataName;
            // If this._allowMultiDialogues is true, more than one qualifier dialogue can be added per term.
            this._qualifierNo = 0;

            var groupOptions = options || {};
            this._allowMultiDialogues = groupOptions.allowMultiDialogues || false;
            this._disableTermDelete = groupOptions.disableTermDelete || false;

            this._dialogueOptions = [];

            this._dialogueMap = {};

            this._crtFocus = null;

            // Builds an empty container for the term.
            this._buildEmptyContainer();
            this._addDialogueHolderListener();
            this._addDialogueFocusObservers();
            this._attachKeyUpObserver();
        },

        /**
         * Returns the constructed dialogue group element for the term.
         *
         * @return {Element|*} the dialogue group element for the term
         */
        get: function() {
            return this._qualifiersContainer;
        },

        /**
         * Associates the dialogue group with some term.
         *
         * @param label {String} the label for the term; must not be null or empty
         * @param termID {String} the ID for the term; if null or empty, will be set to the same value as label
         * @param selectable {Boolean} true iff the term is selectable
         * @return {DetailsDialogueGroup} self
         */
        // TODO:
        //   1. Information button for the term.
        //   2. Label cannot be null or empty; "Add Details" should be disabled if that's the case.
        //   3. Add checkbox select listener: display "Add Details" on select (unless no label provided).
        //   4. If not selectable, "Add Details" should be displayed by default (unless no label provided).
        withLabel: function(label, termID, selectable) {
            var trimmedID = (termID && termID.strip()) || "";
            var trimmedLabel = (label && label.strip()) || "";
            this._termID = trimmedID || trimmedLabel;
            this._label = trimmedLabel;
            if (this._termID === "") {
                return this;
            }
            this._qualifiersContainer.id = this._dataName + "_" + this._termID;
            var termData = this._qualifiersContainer.down('span.term-data');
            var termDataHTML = '<label class="label-field">';
            if (selectable) {
                termDataHTML += '<input id="status_' + this._termID + '" class="term-status" name="' + this._dataName +
                  '" type="checkbox"> ';
            }
            termDataHTML += trimmedLabel + '</label><input class="term-id" type="hidden" value="' + this._termID + '">';
            termData.innerHTML = termDataHTML;
            // Value getter.
            this._initButtons(selectable, false);
            return this;
        },

        /**
         * Associates the dialogue group with data entered through some term picker input. The label and ID for the
         * term will be extracted from user input.
         *
         * @return {DetailsDialogueGroup} self
         */
        // TODO:
        //   1. Validation of input is necessary (non-empty && non-duplicate).
        //   2. If invalid, block save.
        //   3. If invalid, hide "Add Details" button.
        withTermPicker: function() {
            return this;
        },

        dialoguesAddNumericSelect: function(options) {
            var addNumericSelect = function(currentDialogue) {
                currentDialogue.withNumericSelect(options);
            };
            this._dialogueOptions.push(addNumericSelect);
            return this;
        },

        dialoguesAddItemSelect: function(options) {
            var addItemSelect = function(currentDialogue) {
                currentDialogue.withItemSelect(options);
            };
            this._dialogueOptions.push(addItemSelect);
            return this;
        },

        dialoguesAddRadioList: function(collapsible, options) {
            var addRadioList = function(currentDialogue) {
                currentDialogue.withRadioList(collapsible, options);
            };
            this._dialogueOptions.push(addRadioList);
            return this;
        },

        dialoguesAddTextBox: function(collapsible, options) {
            var addTextBox = function(currentDialogue) {
                currentDialogue.withTextBox(collapsible, options);
            };
            this._dialogueOptions.push(addTextBox);
            return this;
        },

        dialoguesAddCustomElement: function(element, collapsible, options) {
            var addCustomElement = function(currentDialogue) {
                currentDialogue.withQualifierElement(element, collapsible, options);
            };
            this._dialogueOptions.push(addCustomElement);
            return this;
        },

        dialoguesAddDeleteAction: function(deleteStr) {
            var addDeleteAction = function(currentDialogue) {
                currentDialogue.withDeleteAction(deleteStr);
            };
            this._dialogueOptions.push(addDeleteAction);
            return this;
        },

        clearDialogueOptions: function() {
            this._dialogueOptions = [];
            return this;
        },

        /**
         * Returns true iff status input is marked as selected (whether by checkbox, or by having data entered).
         * @return {Boolean}
         */
        isAffected: function() {
            var status = this._qualifiersContainer.down('input.term-status');
            if (!status) {
                return !!this._termID;
            } else {
                return status.type === 'checkbox' ? status.checked : !!this._termID;
            }
        },

        setAffected: function(setAffected) {
            var status = this._qualifiersContainer.down('input.term-status');
            status && (status.checked = setAffected);
        },

        getValues: function() {
            var qualifiers = [];
            for (var key in this._dialogueMap) {
                if (this._dialogueMap.hasOwnProperty(key)) {
                    qualifiers.push(this._dialogueMap[key].getValues());
                }
            }
          return {
              "id" : this._termID,
              "label" : this._label,
              "affected" : this.isAffected(),
              "qualifiers" : qualifiers
            };
        },

        setValues: function(values) {
            if (!values) { return; }
            this._clearDetails();
            this._updateIdIfNeeded(values.id);
            this._updateLabelIfNeeded(values.label);
            if (values.affected) {
                this.setAffected(values.affected);
                var qualifiers = values.qualifiers || [];
                var _this = this;
                qualifiers.forEach(function(qualifier) {
                    _this.addDialogue(true).setValues(qualifier);
                });
            }
        },

        clearDetails: function() {
            this._clearDetails();
            Event.fire(this._qualifiersContainer, this._dataName + ':qualifiers:cleared');
        },

        addDialogue: function(silent) {
            var dialogue = this._addDialogue();
            this._dialogueMap[dialogue.getID()] = dialogue;
            this._applyDialogueOptions(dialogue);
            this._addOnClearDetailsListener();
            !this._allowMultiDialogues && this._removeDetailsClickListener();
            if (!silent) {
                Event.fire(this._qualifiersContainer, this._dataName + ':dialogue:added');
                this._crtFocus && this._crtFocus.removeClassName('focused');
                this._crtFocus = dialogue.getDialogue().addClassName('focused');
                Event.fire(this._qualifiersContainer, this._dataName + ':dialogue:focused');
            }
            this._dialogueHolder.show();
            return dialogue;
        },

        size: function() {
            return this._qualifierNo;
        },

        _updateIdIfNeeded: function(newId) {
            // TODO: Change everywhere. Also validate that there are no duplicates?
        },

        _updateLabelIfNeeded: function(newLabel) {
            // TODO: Change everywhere. Also validate that there are no duplicates?
        },

        _clearDetails: function() {
            this._qualifierNo = 0;
            this._dialogueHolder.descendants().forEach(function(elem) {
                elem.stopObserving();
            });
            this._dialogueMap = {};
            this._dialogueHolder.update();
            this._dialogueHolder.hide();
            this._removeDetailsClickListener();
            (this._allowMultiDialogues || this.size() === 0) && this._addDetailsClickListener();
            this._removeOnClearDetailsListener();
        },

        /**
         * Builds an empty container that will hold qualifiers dialogues for a term.
         * @private
         */
        _buildEmptyContainer: function() {
            this._qualifiersContainer = new Element('table', {'class' : 'summary-group'});
            this._qualifiersContainer.name = this._dataName;
            this._qualifiersContainer.innerHTML =
                  '<tbody>' +
                      '<tr class="term-holder">' +
                          '<td>' +
                              '<span class="term-data"></span>' +
                              '<span class="button-holder"></span>' +
                          '</td>' +
                      '</tr>' +
                      '<tr>' +
                          '<td class="dialogue-holder"></td>' +
                      '</tr>' +
                  '</tbody>';
            this._dialogueHolder = this._qualifiersContainer.down('td.dialogue-holder');
            this._dialogueHolder.hide();
        },

        /**
         * Sets the starting state for the "Add Details" and "Delete" buttons.
         *
         * @param selectable {Boolean} true iff the term is selectable
         * @param changeable {Boolean} true iff the term can be entered via text input
         * @private
         */
        _initButtons: function(selectable, changeable) {
            var buttonHolder = this._qualifiersContainer.down('span.button-holder');
            this._addDetailsButton = new Element('span', {'id' : 'add_details_' + this._termID,
              'class' : 'patient-menu-button patient-details-add'}).update('Add Details');
            this._clearDetailsButton = new Element('span', {'id' : 'clear_term_' + this._termID,
              'class' : 'patient-menu-button patient-term-clear'}).update("Clear Details");
            this._deleteButton = new Element('span', {'id' : 'delete_term_' + this._termID,
              'class' : 'patient-menu-button patient-term-delete'}).update('Delete');

            buttonHolder.insert(this._addDetailsButton);
            buttonHolder.insert(this._clearDetailsButton);
            buttonHolder.insert(this._deleteButton);

            // Hide delete button or attach observers.
            if (this._disableTermDelete) {
                this._deleteButton.hide();
            } else {
                this._addTermDeleteListener();
            }

            // Should be hidden initially.
            this._clearDetailsButton.hide();

            if (selectable || changeable) {
                this._addDetailsButton.hide();
                selectable ? this._addOnTermSelectListener() : this._addOnInputChangedListener();
            }
        },

        _applyDialogueOptions: function(dialogue) {
            this._dialogueOptions.forEach(function(applyOption) {
                applyOption(dialogue);
            });
            return dialogue;
        },

        // TODO: Need to make sure that the _termID is not empty.
        _addDialogue: function() {
            var qualifierID = this._termID + "_" + this._qualifierNo++;
            return new DetailsDialogue(qualifierID, this._dataName, this._dialogueHolder).attach();
        },

        /**
         * Listens for click on the delete button.
         * @private
         */
        _addTermDeleteListener: function() {
            var _this = this;
            this._deleteButton.observe('click', function(event) {
                var idInput = _this._qualifiersContainer.down('input.term-id');
                var id = idInput && idInput.value;
                _this._qualifiersContainer.remove();
                _this._dialogueMap = {};
                Event.fire(_this._qualifiersContainer, _this._dataName + ':term:deleted', {'id' : id});
            });
        },

        /**
         * Listens for term being selected.
         * @private
         */
        _addOnTermSelectListener: function() {
            var _this = this;
            var statusInput = this._qualifiersContainer.down('input.term-status');
            statusInput.observe('change', function(event) {
                if (statusInput.checked) {
                    _this._addDetailsClickListener();
                } else {
                    _this.clearDetails();
                    _this._removeDetailsClickListener();
                }
                Event.fire(_this._qualifiersContainer, _this._dataName + ':status:changed', {'id' : _this._termID});
            })
        },

        // TODO: Implement.
        _addOnInputChangedListener: function() {

        },

        _addOnClearDetailsListener: function() {
            this._clearDetailsButton.observe('click', this.clearDetails.bind(this));
            this._clearDetailsButton.show();
        },

        _removeOnClearDetailsListener: function() {
            this._clearDetailsButton.stopObserving('click');
            this._clearDetailsButton.hide();
        },

        _addDetailsClickListener: function() {
            this._addDetailsButton.observe('click', this.addDialogue.bind(this, false));
            this._addDetailsButton.show();
        },

        _removeDetailsClickListener: function() {
            this._addDetailsButton.stopObserving('click');
            this._addDetailsButton.hide();
        },

        _addDialogueHolderListener: function() {
            var _this = this;
            this._dialogueHolder.observe(_this._dataName + ':qualifier:deleted', function(event) {
                event.memo && event.memo.id && (delete _this._dialogueMap[event.memo.id]);
                if (!this.down()) {
                    _this.clearDetails();
                    _this._clearDetailsButton.hide();
                    !_this._allowMultiDialogues && _this._addDetailsClickListener();
                }
            });
        },

        // TODO: This is a monstrosity, refactor.
        _addDialogueFocusObservers: function() {
            var _this = this;
            document.observe('click', function (event) {
                var summaryItem = event.findElement('div.summary-item');
                if (summaryItem) {
                    if (_this._crtFocus) {
                        if (event.findElement('td.dialogue-holder') === _this._dialogueHolder) {
                            _this._crtFocus.removeClassName('focused');
                            Event.fire(_this._qualifiersContainer, _this._dataName + ':dialogue:blurred');
                            _this._crtFocus = summaryItem;
                            _this._crtFocus.addClassName('focused');
                            Event.fire(_this._qualifiersContainer, _this._dataName + ':dialogue:focused');
                        } else {
                            _this._crtFocus.removeClassName('focused');
                            Event.fire(_this._qualifiersContainer, _this._dataName + ':dialogue:blurred');
                            _this._crtFocus = null;
                        }
                    } else {
                        if (event.findElement('td.dialogue-holder') === _this._dialogueHolder) {
                            _this._crtFocus = summaryItem;
                            _this._crtFocus.addClassName('focused');
                            Event.fire(_this._qualifiersContainer, _this._dataName + ':dialogue:focused');
                        }
                    }
                } else {
                    if (_this._crtFocus) {
                        _this._crtFocus.removeClassName('focused');
                        Event.fire(_this._qualifiersContainer, _this._dataName + ':dialogue:blurred');
                        _this._crtFocus = null;
                    }
                }
            })
        },

        _attachKeyUpObserver: function() {
            var _this = this;
            this._dialogueHolder.observe('keyup', function(event) {
                event.target.hasClassName('cancer_notes') &&
                    Event.fire(_this._qualifiersContainer, _this._dataName + ':notes:updated', {'target' : event.target});
            })
        }
    });
    return DetailsDialogueGroup;
});
