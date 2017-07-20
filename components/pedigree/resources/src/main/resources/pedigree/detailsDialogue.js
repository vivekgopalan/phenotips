/**
 * DetailsDialogue allows to build a dialogue for collecting additional "qualifiers" data for some term.
 *
 * @class DetailsDialogue
 * @constructor
 */
// TODO: Move and rename.
define([], function() {
    var DetailsDialogue = Class.create({
        initialize: function (elementID, dataName, dialogueHolder) {
            this._container = dialogueHolder;
            this._dataName = dataName;
            this._elementID = elementID;
            this._qualifierMap = {};
            this._buildEmptyDialogue();
        },

        /**
         * Generates a numeric select element.
         *
         * @param options {Object} an options object containing:
         *                         - "from" the starting value, as integer, for the numeric select
         *                         - "to" the final value, as integer, for the numeric select
         *                         - "step" the step size, as integer
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         * @return {*|DetailsDialogue}
         */
        withNumericSelect: function(options) {
            // Define data ranges for the numeric select element.
            options = options || {};
            var from = options.from || 0;
            var to = options.to || 0;
            var step = options.step;
            var inputSourceClass = options.inputSourceClass || "";
            var spanElem = new Element('span');
            var optionsHTML = '<select name="' + this._dataName + '" class="'+inputSourceClass+'"><option value=""></option>';
            optionsHTML += '<option value="before_' + from + '">before ' + from + '</option>';
            for (var num = from; num <= to; num++) {
                if (num % step == 0) {
                    optionsHTML += '<option value="before_' + num + '">' + (num - step + 1) + '-' + num + '</option>';
                }
                optionsHTML += '<option value="' + num + '">' + num + '</option>';
            }
            optionsHTML += '<option value="after_' + to + '">after ' + to + '</option></select>';
            spanElem.innerHTML = optionsHTML;
            spanElem._addValue = function(values) {
                var fieldName = options.qualifierName || null;
                if (fieldName) {
                    var select = spanElem.down('select');
                    values[fieldName] = select.selectedIndex >= 0 ? select.options[select.selectedIndex].value : "";
                }
            };
            spanElem._setValue = function(value) {
                spanElem.down('select').value = value;
            };
            options.inline = 'inline';
            return this.withQualifierElement(spanElem, false, options);
        },

        /**
         * Generates a select element for an item array.
         *
         * @param options {Object} an options object containing:
         *                         - "data" the select list items, as array
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         */
        withItemSelect: function(options) {
            options = options || {};
            var inputSourceClass = options.inputSourceClass || "";
            var data = options.data || [""];
            var spanElem = new Element('span');
            var optionsHTML = '<select name="' + this._dataName + '" class="'+inputSourceClass+'">';
            data.forEach(function(item) {
                optionsHTML += '<option value="' + item + '">' + item + '</option>';
            });
            optionsHTML += '</select>';
            spanElem.innerHTML = optionsHTML;
            spanElem._addValue = function(values) {
                var fieldName = options.qualifierName || null;
                if (fieldName) {
                    var select = spanElem.down('select');
                    values[fieldName] = select.selectedIndex >= 0 ? select.options[select.selectedIndex].value : "";
                }
            };
            spanElem._setValue = function(value) {
                spanElem.down('select').value = value;
            };
            options.inline = 'inline';
            return this.withQualifierElement(spanElem, false, options);
        },

        /**
         * Generates a list with radio button elements for each item in provided array.
         *
         * @param collapsible true iff the qualifier element is collapsible
         * @param options {Object} an options object containing:
         *                         - "data" the select list items, as array
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         */
        withRadioList: function(collapsible, options) {
            options = options || {};
            var _this = this;
            var inputSourceClass = options.inputSourceClass || "";
            var data = options.data || [];
            var spanElem = new Element('span');
            var radioHTML = '<ul>';
            data.forEach(function(item) {
                var inputId = inputSourceClass + "_" + _this._elementID + "_" + item;
                radioHTML +=
                    '<li class="term-entry">' +
                      '<input class="' + inputSourceClass + '" id="' + inputId + '" name="' + _this._dataName + '_' + _this._elementID + '_' + options.qualifierName + '" title="' + item + '" type="radio">' +
                      '<label for="' + inputId + '" title="' + item + '">' + item + '</label>' +
                    '</li>'
            });
            radioHTML += '</ul>';
            spanElem.innerHTML = radioHTML;
            spanElem.down("li.term-entry").down('input').checked=true;
            spanElem._addValue = function(values) {
                var fieldName = options.qualifierName || null;
                if (fieldName) {
                    values[fieldName] = spanElem.down('input[name="' + _this._dataName + '_' + _this._elementID + '_' + options.qualifierName + '"]:checked').title;
                }
            };
            spanElem._setValue = function(value) {
                var selection = spanElem.down('input[title="' + value + '"]');
                selection.checked = true;
                // TODO: fire update label.
            };
            return this.withQualifierElement(spanElem, collapsible, options);
        },

        /**
         * Generates and adds a text-box.
         *
         * @param collapsible true iff the qualifier element is collapsible
         * @param options {Object} an options object containing:
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         *                         - "inputSourceClass" the css class for the input source element
         */
        withTextBox: function(collapsible, options) {
            var inputSourceClass = options && options.inputSourceClass || "";
            var spanElem = new Element('span');
            var textArea = new Element('textarea', {'name' : this._dataName, 'class' : inputSourceClass});
            spanElem.update(textArea);
            spanElem._addValue = function(values) {
                var fieldName = options.qualifierName || null;
                if (fieldName) {
                    values[fieldName] = spanElem.down('textarea').value;
                }
            };
            spanElem._setValue = function(value) {
                spanElem.down('textarea').value = value;
            };
            return this.withQualifierElement(spanElem, collapsible, options);
        },

        /**
         * Adds a custom qualifier definition list element.
         *
         * @param element {Element} the custom data collection element to add (e.g. a textbox)
         * @param collapsible {Boolean} true iff this is a collapsible element
         * @param options {Object} an options object containing:
         *                         - "defListItemClass" the css class for the definition list item
         *                         - "qualifierLabel" the label for the qualifier definition list element
         *                         - "qualifierName" the name of the qualifier definition list element
         * @return {DetailsDialogue}
         */
        withQualifierElement: function(element, collapsible, options) {
            var defListItemClass = options && options.defListItemClass || "";
            var qualifierLabel = options && options.qualifierLabel || "";
            var qualifierName = options && options.qualifierName || "";
            var dtElem = new Element('dt');
            var ddElem = new Element('dd');
            dtElem.addClassName(defListItemClass);
            if (options && options.inline) {
                dtElem.addClassName(options.inline);
                ddElem.addClassName(options.inline);
            }
            ddElem.addClassName(defListItemClass);
            var selectedValue = new Element('span', {'class': 'selected-value'});
            if (collapsible) {
                dtElem.addClassName("collapsible");
                ddElem.addClassName("collapsed");
                dtElem.insert(new Element('span', {'class' : 'collapse-button'}).update("►"));
                var termEntry = element.down("li.term-entry");
                var termInput = termEntry && termEntry.down('input');
                selectedValue.update(termInput && termInput.title || "");
                ddElem.hide();
            }
            dtElem.insert(new Element('label').update(qualifierLabel));
            dtElem.insert(selectedValue);
            ddElem.insert(element);

            this._qualifierList.insert(dtElem).insert(ddElem);
            qualifierName && (this._qualifierMap[qualifierName] = element);
            return this;
        },

        /**
         * Adds a delete action element to the dialogue.
         *
         * @param removeStr the removal url
         * @return {DetailsDialogue}
         */
        withDeleteAction: function(removeStr) {
            removeStr && this._termDetails.insert(
                new Element('input', {'id' : 'delete_action_' + this._elementID, 'name' : 'delete-action',
                  'value' : removeStr, 'type' : 'hidden'})
            );
            var deleteAction = new Element('span', {'id' : 'delete_' + this._elementID, 'class' : 'action-done'})
              .update("×");
            this._termDetails.insert({top: deleteAction});
            this._attachOnDeleteListener(deleteAction);
            return this;
        },

        /**
         * Attaches the dialogue to the parent container (this._container).
         *
         * @return {DetailsDialogue} self
         */
        attach: function() {
            this._container.insert(this._termDetails);
            return this;
        },

        /**
         * Returns the constructed dialogue element.
         *
         * @return {Element|*} the constructed dialogue element
         */
        getDialogue: function() {
            return this._termDetails;
        },

        /**
         * Gets the qualifier ID.
         *
         * @return {String} the qualifier ID
         */
        getID: function() {
            return this._elementID;
        },

        /**
         * Gets the values for all the input sources in the dialogue.
         *
         * @return {Object} containing the custom element name to value mapping
         */
        getValues: function() {
            var values = {};
            for (var key in this._qualifierMap) {
                if (this._qualifierMap.hasOwnProperty(key)) {
                  var element = this._qualifierMap[key];
                  element._addValue(values);
                }
            }
            return values;
        },

        /**
         * Sets values for the dialogue.
         *
         * @param values {Object} the values are key-value pairs, where the keys should be the same as in _qualifierMap
         */
        setValues: function(values) {
            if (!values) { return; }
            for (var key in values) {
                if (values.hasOwnProperty(key)) {
                    var elem = this._qualifierMap[key];
                    if (elem) {
                        elem._setValue(values[key]);
                    }
                }
            }
        },

        /**
         * Builds an empty dialogue where qualifiers will be added.
         * @private
         */
        _buildEmptyDialogue: function () {
            this._termDetails = new Element('div', {'class' : 'summary-item'});
            this._termDetails.innerHTML =
              '<div id="term_details_' + this._elementID + '" class="term-details">' +
                  '<dl></dl>' +
              '</div>';

            this._qualifierList = this._termDetails.down('dl');
            // Attach listeners for the collapse action.
            this._attachOnClickListener();
        },

        _attachOnDeleteListener: function(deleteAction) {
            // TODO: Implement (if a deleteStr is provided).
            var _this = this;
            deleteAction.observe('click', function(event) {
                _this._termDetails.remove();
                _this._qualifierMap = {};
                Event.fire(_this._container, _this._dataName + ':qualifier:deleted', {'id' : _this._elementID});
            });
        },

        /**
         * Attaches an onclick listener to observe click events on specified elements.
         * @private
         */
        _attachOnClickListener: function() {
            var _this = this;
            this._termDetails.observe('click', function(event) {
                var elem = event.target.up();
                var ddElem;
                // The on-click event is to collapse/un-collapse an element
                if (elem && elem.hasClassName('collapsible')) {
                    ddElem = elem.next('dd');
                    ddElem && _this._toggleCollapsed(elem, ddElem);
                // The on-click event is to select some list item
                } else if (elem && elem.hasClassName('term-entry')) {
                    ddElem = elem.up('dd');
                    ddElem && _this._updateLabelWithSelection(elem, ddElem.previous('dt'));
                }
            });
        },

        /**
         * Updates the definition list element label with the current selection.
         *
         * @param selectedElem the current selection made by user
         * @param dtElem the element holding the label
         * @private
         */
        _updateLabelWithSelection: function(selectedElem, dtElem) {
            var currentValHolder = dtElem && dtElem.down('span.selected-value');
            if (currentValHolder) {
                var elInput = selectedElem.down('input');
                var selectedValue = elInput && elInput.title || "";
                currentValHolder.update(selectedValue);
                if (selectedValue === "Unknown" || selectedValue === "") {
                    currentValHolder.removeClassName("selected");
                } else {
                    currentValHolder.addClassName("selected");
                }
            }
        },

        /**
         * Toggles collapsed for a collapsible element.
         *
         * @param dtElem the element containing the collapse button
         * @param ddElem the element containing the data to be collapsed
         * @private
         */
        _toggleCollapsed: function(dtElem, ddElem) {
            var collapseSpan = dtElem.down('span.collapse-button');
            if (ddElem.hasClassName('collapsed')) {
                ddElem.removeClassName('collapsed');
                collapseSpan.innerHTML = "▼";
                ddElem.show();
            } else {
                ddElem.addClassName('collapsed');
                collapseSpan.innerHTML = "►";
                ddElem.hide();
            }
        }
    });

    return DetailsDialogue;
});
