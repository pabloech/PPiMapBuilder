package tk.nomis_tech.ppimapbuilder;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.task.edit.MapTableToNetworkTablesTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.LineTypeVisualProperty;
import org.cytoscape.view.presentation.property.NodeShapeVisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;
import org.cytoscape.view.vizmap.mappings.PassthroughMapping;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import tk.nomis_tech.ppimapbuilder.data.settings.PMBSettings;
import tk.nomis_tech.ppimapbuilder.networkbuilder.PMBInteractionNetworkBuildTaskFactory;
import tk.nomis_tech.ppimapbuilder.data.settings.PMBSettingSaveTaskFactory;
import tk.nomis_tech.ppimapbuilder.ui.credits.CreditFrame;
import tk.nomis_tech.ppimapbuilder.ui.querywindow.QueryWindow;
import tk.nomis_tech.ppimapbuilder.ui.resultpanel.ResultPanel;
import tk.nomis_tech.ppimapbuilder.ui.resultpanel.listener.ResultPanelAction;
import tk.nomis_tech.ppimapbuilder.ui.settingwindow.SettingWindow;

import java.awt.*;
import java.util.Properties;

/**
 * The starting point of the plug-in
 */
public class PMBActivator extends AbstractCyActivator {

	public static BundleContext context;

	public PMBActivator() {
		super();
	}

	/**
	 * This methods register all services of PPiMapBuilder
	 *
	 * @param bc
	 */
	@Override
	public void start(BundleContext bc) {
		context = bc;

		OpenBrowser openBrowser = getService(bc, OpenBrowser.class);

		// QueryWindow
		QueryWindow queryWindow = new QueryWindow();
		SettingWindow settingWindow = new SettingWindow();
		CreditFrame creditWindow = new CreditFrame(openBrowser);

		// Will load PMBSettings
		PMBSettings.getInstance();

		// Task factory
		PMBInteractionNetworkBuildTaskFactory createNetworkfactory;
		PMBSettingSaveTaskFactory saveSettingFactory;
		PMBCreditMenuTaskFactory creditMenuFactory;
		{
			TaskManager networkBuildTaskManager = getService(bc, TaskManager.class);

			// Network services
			CyNetworkNaming cyNetworkNamingServiceRef = getService(bc, CyNetworkNaming.class);
			CyNetworkFactory cyNetworkFactoryServiceRef = getService(bc, CyNetworkFactory.class);
			CyNetworkManager cyNetworkManagerServiceRef = getService(bc, CyNetworkManager.class);
			CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);

			// Result panel (protein & interaction s)
			CySwingApplication cytoscapeDesktopService = getService(bc, CySwingApplication.class);
			cytoscapeDesktopService.getCytoPanel(CytoPanelName.EAST).setState(CytoPanelState.DOCK);

			ResultPanel pmbResultPanel = new ResultPanel(openBrowser);
			registerService(bc, pmbResultPanel, CytoPanelComponent.class, new Properties());

			int index = cytoscapeDesktopService.getCytoPanel(CytoPanelName.EAST).indexOfComponent(pmbResultPanel);
			if (index > 0)
				cytoscapeDesktopService.getCytoPanel(CytoPanelName.EAST).setSelectedIndex(index);

			// Result panel action
			ResultPanelAction rpa = new ResultPanelAction(pmbResultPanel, cyApplicationManager);
			registerService(bc, rpa, RowsSetListener.class, new Properties());

			// View services
			CyNetworkViewFactory cyNetworkViewFactoryServiceRef = getService(bc, CyNetworkViewFactory.class);
			CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc, CyNetworkViewManager.class);

			// Layout services
			CyLayoutAlgorithmManager layoutManagerServiceRef = getService(bc, CyLayoutAlgorithmManager.class);

			// Visual Style services
			VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
			VisualMappingFunctionFactory vmfFactoryD = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
			VisualMappingFunctionFactory vmfFactoryP = getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");

			VisualStyleFactory visualStyleFactoryServiceRef = getService(bc, VisualStyleFactory.class);


			//VISUAL STYLE
			// If the style already existed, remove it first
			for (VisualStyle curVS : visualMappingManager.getAllVisualStyles()) {
				if (curVS.getTitle().equalsIgnoreCase("PPiMapBuilder Visual Style")) {
					visualMappingManager.removeVisualStyle(curVS);
					break;
				}
			}

			// Create a new Visual style
			VisualStyle vs = visualStyleFactoryServiceRef.createVisualStyle("PPiMapBuilder Visual Style");
			VisualStyle vsDefault = visualMappingManager.getDefaultVisualStyle();


			//NODE
			//vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, vsDefault.getDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR));
			//vs.setDefaultValue(BasicVisualLexicon.NODE_FILL_COLOR, new Color(255, 255, 255)); // Node color
			vs.setDefaultValue(BasicVisualLexicon.NODE_SHAPE, NodeShapeVisualProperty.ROUND_RECTANGLE);
			vs.setDefaultValue(BasicVisualLexicon.NODE_BORDER_WIDTH, 1.5);
			vs.setDefaultValue(BasicVisualLexicon.NODE_BORDER_PAINT, Color.BLACK);
			vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_COLOR, new Color(51, 153, 255));
			vs.setDefaultValue(BasicVisualLexicon.NODE_LABEL_FONT_SIZE, 10);
			vs.setDefaultValue(BasicVisualLexicon.NODE_SELECTED_PAINT, new Color(160, 255, 144));
			PassthroughMapping pMapping = (PassthroughMapping) vmfFactoryP.createVisualMappingFunction("gene_name", String.class, BasicVisualLexicon.NODE_LABEL);
			vs.addVisualMappingFunction(pMapping);
			DiscreteMapping dMapping = (DiscreteMapping) vmfFactoryD.createVisualMappingFunction("queried", String.class, BasicVisualLexicon.NODE_FILL_COLOR);
			dMapping.putMapValue("true", new Color(255, 255, 51));
			dMapping.putMapValue("false", new Color(255, 255, 255));
			vs.addVisualMappingFunction(dMapping);

			//EDGE
			vs.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT, new Color(204, 204, 204));
			vs.setDefaultValue(BasicVisualLexicon.EDGE_STROKE_SELECTED_PAINT, new Color(255, 0, 0));
			dMapping = (DiscreteMapping) vmfFactoryD.createVisualMappingFunction("interolog", String.class, BasicVisualLexicon.EDGE_LINE_TYPE);
			dMapping.putMapValue("true", LineTypeVisualProperty.EQUAL_DASH);
			dMapping.putMapValue("false", LineTypeVisualProperty.SOLID);
			vs.addVisualMappingFunction(dMapping);

			visualMappingManager.addVisualStyle(vs);


			// Data Table management
			CyTableFactory tableFactory = getService(bc, CyTableFactory.class);
			MapTableToNetworkTablesTaskFactory mapTableToNetworkTablesTaskFactory = getService(bc, MapTableToNetworkTablesTaskFactory.class);

			// Network creation task factory
			createNetworkfactory = new PMBInteractionNetworkBuildTaskFactory(cyNetworkNamingServiceRef, cyNetworkFactoryServiceRef, cyNetworkManagerServiceRef,
					cyNetworkViewFactoryServiceRef, cyNetworkViewManagerServiceRef, layoutManagerServiceRef, visualMappingManager, queryWindow, tableFactory,
					mapTableToNetworkTablesTaskFactory);
			queryWindow.setCreateNetworkfactory(createNetworkfactory);
			registerService(bc, createNetworkfactory, TaskFactory.class, new Properties());
			queryWindow.setTaskManager(networkBuildTaskManager);

			// Save settings task factory
			saveSettingFactory = new PMBSettingSaveTaskFactory();
			settingWindow.setSaveSettingFactory(saveSettingFactory);
			registerService(bc, saveSettingFactory, TaskFactory.class, new Properties());
			settingWindow.setTaskManager(networkBuildTaskManager);

			// Credit task factory
			creditMenuFactory = new PMBCreditMenuTaskFactory(creditWindow);
			registerService(bc, creditMenuFactory, TaskFactory.class, new Properties());

		}

		// Query window menu
		PMBQueryMenuTaskFactory queryWindowTaskFactory = new PMBQueryMenuTaskFactory(queryWindow);
		Properties props = new Properties();
		props.setProperty("preferredMenu", "Apps.PPiMapBuilder");
		props.setProperty("title", "Query");
		registerService(bc, queryWindowTaskFactory, TaskFactory.class, props);

		// Setting window menu
		PMBSettingMenuTaskFactory settingsWindowTaskFactory = new PMBSettingMenuTaskFactory(settingWindow);
		props = new Properties();
		props.setProperty("preferredMenu", "Apps.PPiMapBuilder");
		props.setProperty("title", "Settings");
		registerService(bc, settingsWindowTaskFactory, TaskFactory.class, props);

		// Credits window menu
		PMBCreditMenuTaskFactory creditWindowTaskFactory = new PMBCreditMenuTaskFactory(creditWindow);
		props = new Properties();
		props.setProperty("preferredMenu", "Apps.PPiMapBuilder");
		props.setProperty("title", "Credits");
		registerService(bc, creditWindowTaskFactory, TaskFactory.class, props);

		System.out.println("[PPiMapBuilder] Started.");
	}
}
