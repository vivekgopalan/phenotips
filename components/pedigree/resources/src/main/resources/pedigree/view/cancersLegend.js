/**
 * Class responsible for keeping track of cancers and their properties.
 * This information is graphically displayed in a 'Legend' box.
 *
 * @class CancerLegend
 * @constructor
 */
define([
        "pedigree/hpoTerm",
        "pedigree/model/helpers",
        "pedigree/view/legend"
    ], function(
        HPOTerm,
        Helpers,
        Legend
    ){
    var CancerLegend = Class.create( Legend, {

        initialize: function($super) {
            this._customCancerColors = { "HP:0100013": "#e267a3",  // pink e762a4
                                         "HP:0100615": "#9370DB",  // purple
                                         "HP:0100273": "#945d34",  // brown
                                         "HP:0010784": "#c93320",  // red
                                         "HP:0100787": "#ecb739",  // yellow
                                         "HP:0002894": "#4657dc",  // blue
                                         "HP:0012056": "#444444",  // black (#333333 and darker do not work with raphael gradients)
                                         "HP:0009726": "#197419",  // green
                                         "HP:0006753": "#9aac8c",  // (?) greenish
                                         "HP:0100526": "#008080",  // teal
                                         "HP:0030692": "#F5DEB3",  // (?) wheat
                                         "HP:0100751": "#BC8F8F",  // (?) rosybrown
                                         "HP:0100031": "#FFFF00",  // yellow
                                         "HP:0002896": "#770000",  // dark red
                                         "HP:0030079": "#FFCCCC",  // light pink
                                         "HP:0006775": "#FF0000",  // red 1
                                         "HP:0001909": "#888888"   // gray
                                       };
            this._customCancerLabels = { "Breast": "Breast cancer",
                                         "Ovarian": "Ovarian cancer",
                                         "Colon": "Colorectal cancer",
                                         "Uterus": "Uterus cancer",
                                         "Prostate": "Prostate cancer",
                                         "Pancreatic": "Pancreatic cancer",
                                         "Melanoma": "Melanoma",
                                         "Kidney": "Kidney cancer",
                                         "Gastric": "Gastric cancer",
                                         "Lung": "Lung cancer",
                                         "Brain": "Brain cancer",
                                         "Oesophagus": "Oesophagus cancer",
                                         "Thyroid": "Thyroid cancer",
                                         "Liver": "Liver cancer",
                                         "Cervix": "Cervical cancer",
                                         "Myeloma": "Myeloma",
                                         "Leukemia": "Leukemia"
                                       };
            this._customLabelMapping = { "HP:0100013": "Breast",
                                         "HP:0100615": "Ovarian",
                                         "HP:0100273": "Colorectal",
                                         "HP:0010784": "Uterus",
                                         "HP:0100787": "Prostate",
                                         "HP:0002894": "Pancreatic",
                                         "HP:0012056": "Melanoma",
                                         "HP:0009726": "Kidney",
                                         "HP:0006753": "Gastric",
                                         "HP:0100526": "Lung",
                                         "HP:0030692": "Brain",
                                         "HP:0100751": "Oesophagus",
                                         "HP:0100031": "Thyroid",
                                         "HP:0002896": "Liver",
                                         "HP:0030079": "Cervical",
                                         "HP:0006775": "Myeloma",
                                         "HP:0001909": "Leukemia"
                                       };

            $super('Cancers', 'cancers', true);

            this._termCache = {};
        },

        _getPrefix: function(id) {
            return "cancers";
        },

        _getAllSupportedCancers: function() {
            var clist = [];
            for (var cancer in this._customCancerColors) {
                if (this._customCancerColors.hasOwnProperty(cancer)) {
                    clist.push(cancer);
                }
            }
            return clist;
        },

        /**
         * Returns the HPOTerm object for the cancer with the given ID. If object is not in cache yet
         * returns a newly created one which may have the cancer name & other attributes not loaded yet
         *
         * @method getCancer
         * @return {Object}
         */
        getCancer: function(cancerID) {
            if (!this._termCache.hasOwnProperty(cancerID)) {
                var whenNameIsLoaded = function() { this._updateCancerName(cancerId); };
                var name = this._customLabelMapping.hasOwnProperty(cancerID) ? this._customLabelMapping[cancerID] : null;
                this._termCache[cancerID] = new HPOTerm(cancerID, name, whenNameIsLoaded.bind(this));
            }
            return this._termCache[cancerID];
        },

        /**
         * Returns cancer name
         *
         * @param {Number|String} cancerID for this cancer
         * @return {String} associated cancer name
         */
        getName: function($super, cancerID) {
            var stored = this.getCancer(cancerID).getName();
            return this._customCancerLabels.hasOwnProperty(stored) ? this._customCancerLabels[stored] : stored;
        },

        /**
         * Registers an occurrence of a cancer. If disorder hasn't been documented yet,
         * designates a color for it.
         *
         * @method addCase
         * @param {Number|String} id ID for this cancer taken from the HPO database
         * @param {String} name The name of the cancer
         * @param {Number} nodeID ID of the Person who has this disorder
         */
        addCase: function($super, id, name, nodeID) {
            if (!this._termCache.hasOwnProperty(id))
                this._termCache[id] = new HPOTerm(id, name);

            $super(id, name, nodeID);
        },

        /**
         * Updates the displayed cancer name for the given cancer
         *
         * @method _updateCancerName
         * @param {Number} id The identifier of the cancer to update
         * @private
         */
        _updateCancerName: function(id) {
            //console.log("updating phenotype display for " + id + ", name = " + this.getTerm(id).getName());
            var _this = this;
            var name = this._legendBox.down('li#' + this._getPrefix() + '-' + _this._hashID(id) + ' .disorder-name');
            name.update(this.getCancer(id).getName());
        },

        /**
         * Generate the element that will display information about the given cancer in the legend
         *
         * @method _generateElement
         * @param {Number} cancerID The id for the disorder, taken from the OMIM database
         * @param {String} name The human-readable disorder name
         * @return {HTMLLIElement} List element to be insert in the legend
         */
        _generateElement: function($super, cancerID, name) {
            if (!this._objectColors.hasOwnProperty(cancerID)) {
                var color = this._generateColor(cancerID);
                this.setObjectColor(cancerID, color);
                document.fire('cancer:color', {'id' : cancerID, color: color});
            }
            return $super(cancerID, name);
        },

        /**
         * Callback for dragging an object from the legend onto nodes
         *
         * @method _onDropGeneric
         * @param {Person} Person node
         * @param {String|Number} id ID of the disorder being dropped
         */
        _onDropObject: function($super, node, cancerID) {
            if (!$super(node, cancerID)) {
                return false;
            }
            var currentCancers = Helpers.cloneObject(node.getCancers());
            // only if the node does not have this cancer yet (either "not tested" or "unaffected")
            if (!currentCancers.hasOwnProperty(cancerID) || !currentCancers[cancerID].affected) {
                currentCancers[cancerID] = {"affected": true};
                editor.getView().unmarkAll();
                var properties = { "setCancers": currentCancers };
                var event = { "nodeID": node.getID(), "properties": properties };
                document.fire("pedigree:node:setproperty", event);
            } else {
                this._onFailedDrag(node, "This person is already marked as affected by the selected cancer", "Can't drag this cancer to this person");
            }
        },

        /**
         * Generates a CSS color.
         * Has preference for some predefined colors that can be distinguished in gray-scale
         * and are distinct from gene colors.
         *
         * @method generateColor
         * @return {String} CSS color
         */
        _generateColor: function(cancerID) {
            if(this._objectColors.hasOwnProperty(cancerID)) {
                return this._objectColors[cancerID];
            }
            if(this._customCancerColors.hasOwnProperty(cancerID)) {
                return this._customCancerColors[cancerID];
            }
            var usedColors = Object.values(this._objectColors);
            // red/yellow gamma
            var prefColors = ['#f8ebb7', '#eac080', '#bf6632', '#a47841', '#c95555', '#ae6c57'];
            usedColors.each( function(color) {
                prefColors = prefColors.without(color);
            });
            if(prefColors.length > 0) {
                return prefColors[0];
            }
            else {
                var randomColor = Raphael.getColor();
                while(randomColor == "#ffffff" || usedColors.indexOf(randomColor) != -1) {
                    randomColor = "#"+((1<<24)*Math.random()|0).toString(16);
                }
                return randomColor;
            }
        }
    });
    return CancerLegend;
});
