package org.drools.brms.client.ruleeditor;

import java.util.HashMap;
import java.util.Map;

import org.drools.brms.client.common.AssetFormats;
import org.drools.brms.client.common.GenericCallback;
import org.drools.brms.client.common.LoadingPopup;
import org.drools.brms.client.decisiontable.DecisionTableXLSWidget;
import org.drools.brms.client.modeldriven.ui.RuleModeller;
import org.drools.brms.client.packages.ModelAttachmentFileWidget;
import org.drools.brms.client.packages.SuggestionCompletionCache;
import org.drools.brms.client.rpc.RepositoryServiceFactory;
import org.drools.brms.client.rpc.RuleAsset;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This launches the appropriate editor for the asset type.
 * This uses the format attribute to determine the appropriate editor, and
 * ALSO to unpackage the content payload from the generic asset RPC object.
 * 
 * NOTE: when adding new editors for asset types, this will also need to be enhanced to load 
 * it up/unpackage it correctly for the editor.
 * The editors will make changes to the rpc objects in place, and when checking in the whole RPC 
 * objects will be sent back to the server.
 * 
 * @author Michael Neale
 */
public class EditorLauncher {

    
    public static final Map TYPE_IMAGES = getTypeImages();
    
    /**
     * This will return the appropriate viewer for the asset.
     */
    public static Widget getEditorViewer(RuleAsset asset,
                                         RuleViewer viewer) {
        //depending on the format, load the appropriate editor
        if ( asset.metaData.format.equals( AssetFormats.BUSINESS_RULE ) ) {
            return new RuleValidatorWrapper( new RuleModeller( asset  ), asset);
        } else if ( asset.metaData.format.equals( AssetFormats.DSL_TEMPLATE_RULE ) ) {
            return new RuleValidatorWrapper(new DSLRuleEditor( asset ), asset);
        } else if ( asset.metaData.format.equals( AssetFormats.MODEL ) ) {
            return new ModelAttachmentFileWidget( asset, viewer );
        } else if (asset.metaData.format.equals( AssetFormats.DECISION_SPREADSHEET_XLS )){
            return new RuleValidatorWrapper(new DecisionTableXLSWidget( asset, viewer ), asset);
        } else if (asset.metaData.format.equals( AssetFormats.RULE_FLOW_RF )) {
            return new RuleFlowUploadWidget(asset, viewer);
        } else {
            return new RuleValidatorWrapper(new DefaultRuleContentWidget( asset ), asset);
        }

    }


    private static Map getTypeImages() {
        Map result = new HashMap();
        
        result.put( AssetFormats.DRL, "technical_rule_assets.gif" );
        result.put( AssetFormats.DSL, "dsl.gif" );
        result.put( AssetFormats.FUNCTION, "function_assets.gif" );
        result.put( AssetFormats.MODEL, "model_asset.gif" );
        result.put( AssetFormats.DECISION_SPREADSHEET_XLS, "spreadsheet_small.gif" );
        result.put( AssetFormats.BUSINESS_RULE, "rule_asset.gif" );
        result.put( AssetFormats.DSL_TEMPLATE_RULE, "rule_asset.gif" );
        result.put( AssetFormats.RULE_FLOW_RF, "ruleflow_small.gif" );
        
        
        return result;
    }
    
    /**
     * Get the icon name (not the path), including the extension, for the appropriate
     * asset format.
     */
    public static String getAssetFormatIcon(String format) {
        String result = (String) TYPE_IMAGES.get( format );
        if (result == null) {
            return "rule_asset.gif";
        } else {
            return result;
        }
    }


    /**
     * This will show the rule viewer. If it was previously opened, it will show that dialog instead
     * of opening it again.
     */
    public static void showLoadEditor(final Map openedViewers,
                                      final TabPanel tab,
                                      final String uuid,
                                      final boolean readonly) {

        if ( openedViewers.containsKey( uuid ) ) {
            tab.selectTab( tab.getWidgetIndex( (Widget) openedViewers.get( uuid ) ) );
            LoadingPopup.close();
            return;
        }

        RepositoryServiceFactory.getService().loadRuleAsset( uuid,
                                                             new GenericCallback() {

                                                                 public void onSuccess(Object o) {
                                                                     final RuleAsset asset = (RuleAsset) o;

                                                                     SuggestionCompletionCache cache = SuggestionCompletionCache.getInstance();
                                                                     cache.doAction( asset.metaData.packageName,
                                                                                     new Command() {
                                                                                         public void execute() {
                                                                                             openRuleViewer( openedViewers,
                                                                                                             tab,
                                                                                                             uuid,
                                                                                                             readonly,
                                                                                                             asset );
                                                                                         }

                                                                                     } );
                                                                 }

                                                             } );

    }

    /**
     * This will actually show the viewer once everything is loaded and ready.
     * @param openedViewers
     * @param tab
     * @param uuid
     * @param readonly
     * @param asset
     */
    private static void openRuleViewer(final Map openedViewers,
                                       final TabPanel tab,
                                       final String uuid,
                                       final boolean readonly,
                                       RuleAsset asset) {
        final RuleViewer view = new RuleViewer( asset,
                                                readonly );

        String displayName = asset.metaData.name;
        if ( displayName.length() > 10 ) {
            displayName = displayName.substring( 0,
                                                 7 ) + "...";
        }
        String icon = getAssetFormatIcon( asset.metaData.format );
        
        tab.add( view,
                 "<img src='images/" + icon + "'>" + displayName,
                 true );

        openedViewers.put( uuid,
                           view );

        view.setCloseCommand( new Command() {
            public void execute() {
                tab.remove( tab.getWidgetIndex( view ) );
                tab.selectTab( 0 );
                openedViewers.remove( uuid );

            }
        } );
        tab.selectTab( tab.getWidgetIndex( view ) );
    }

    
    
    
}
