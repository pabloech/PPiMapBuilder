package tk.nomis_tech.ppimapbuilder.networkbuilder.network;

import com.eclipsesource.json.JsonObject;
import org.cytoscape.model.*;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import tk.nomis_tech.ppimapbuilder.data.organism.Organism;
import tk.nomis_tech.ppimapbuilder.data.protein.Protein;
import tk.nomis_tech.ppimapbuilder.data.protein.UniProtEntry;
import tk.nomis_tech.ppimapbuilder.data.protein.UniProtEntryCollection;
import tk.nomis_tech.ppimapbuilder.networkbuilder.PMBInteractionNetworkBuildTaskFactory;
import tk.nomis_tech.ppimapbuilder.ui.querywindow.QueryWindow;
import tk.nomis_tech.ppimapbuilder.data.client.web.interaction.PsicquicResultTranslator;
import uk.ac.ebi.enfin.mi.cluster.EncoreInteraction;

import java.util.*;

public class PMBCreateNetworkTask extends AbstractTask {

	// For the network
	private final CyNetworkManager networkManager;
	private final CyNetworkFactory networkFactory;
	private final CyNetworkNaming namingUtil;

	// For the view
	private final CyNetworkViewFactory viewFactory;
	private final CyNetworkViewManager viewManager;

	// For the layout
	private final CyLayoutAlgorithmManager layoutManager;

	// For the visual style
	private final VisualMappingManager vizMapManager;
	

	private final Organism referenceOrganism;
	private final HashMap<Organism, Collection<EncoreInteraction>> interactionsByOrg;
	private final UniProtEntryCollection interactorPool;
	private final UniProtEntryCollection proteinOfInterestPool;
	private final PMBInteractionNetworkBuildTaskFactory pmbInteractionNetworkBuildTaskFactory;

	//Network data
	private final HashMap<String, CyNode> nodeNameMap;
	private final long startTime;

	public PMBCreateNetworkTask(
			final PMBInteractionNetworkBuildTaskFactory pmbInteractionNetworkBuildTaskFactory,
			final CyNetworkManager netMgr,
			final CyNetworkNaming namingUtil,
			final CyNetworkFactory cnf,
			final CyNetworkViewFactory cnvf,
			final CyNetworkViewManager networkViewManager,
			final CyLayoutAlgorithmManager layoutMan,
			final VisualMappingManager vmm,
			final HashMap<Organism, Collection<EncoreInteraction>> interactionsByOrg,
			final UniProtEntryCollection interactorPool,
			final UniProtEntryCollection proteinOfInterestPool,
			final QueryWindow queryWindow,
			long startTime
	) {
		this.pmbInteractionNetworkBuildTaskFactory = pmbInteractionNetworkBuildTaskFactory;
		
		// For the network
		this.networkManager = netMgr;
		this.networkFactory = cnf;
		this.namingUtil = namingUtil;

		// For the view
		this.viewFactory = cnvf;
		this.viewManager = networkViewManager;

		// For the layout
		this.layoutManager = layoutMan;

		// For the visual style
		this.vizMapManager = vmm;

		this.interactionsByOrg = interactionsByOrg;
		this.interactorPool = interactorPool;
		this.proteinOfInterestPool = proteinOfInterestPool;
		this.referenceOrganism = queryWindow.getSelectedRefOrganism();
		
		this.nodeNameMap = new HashMap<String, CyNode>();
		
		this.startTime = startTime;
	}

	@Override
	public void run(TaskMonitor monitor) {

		monitor.setTitle("PPiMapBuilder interaction network build");

		monitor.setStatusMessage("Building Cytoscape network...");
		monitor.setProgress(1.0);
		
		if (!interactionsByOrg.get(referenceOrganism).isEmpty() && !interactionsByOrg.isEmpty()) {
			
			// Create an empty network
			CyNetwork network = networkFactory.createNetwork();
			network.getRow(network).set(CyNetwork.NAME, namingUtil.getSuggestedNetworkTitle("PPiMapBuilder network"));
			
			CyTable networkTable = network.getDefaultNetworkTable();
			networkTable.createColumn("created by", String.class, true);
			networkTable.createColumn("build time (seconds)", Integer.class, true);
			network.getRow(network).set("created by", "PPiMapBuilder");

			//Create nodes using interactors pool
			createNodes(network);

			//Create edges using reference interactions and organism interactions
			createEdges(network);

			// Creation on the view
			CyNetworkView view = applyView(network);

			// Layout
			applyLayout(view);

			// Visual Style
			applyVisualStyle(view);

			network.getRow(network).set("build time (seconds)", (((int)(System.currentTimeMillis()- startTime))/1000));
		}
	}
	
	private void createNodes(CyNetwork network) {

		// Node attributes
		CyTable nodeTable = network.getDefaultNodeTable();
		nodeTable.createColumn("uniprot_id", String.class, false);
		nodeTable.createColumn("gene_name", String.class, false);
		nodeTable.createColumn("ec_number", String.class, false);
		nodeTable.createListColumn("synonym_gene_names", String.class, false);
		nodeTable.createColumn("protein_name", String.class, false);
		nodeTable.createColumn("tax_id", String.class, false);
		nodeTable.createColumn("reviewed", String.class, false);
		nodeTable.createListColumn("cellular_components_hidden", String.class, false);
		nodeTable.createListColumn("biological_processes_hidden", String.class, false);
		nodeTable.createListColumn("molecular_functions_hidden", String.class, false);
		nodeTable.createListColumn("orthologs", String.class, false);
		nodeTable.createListColumn("cellular_components", String.class, false);
		nodeTable.createListColumn("biological_processes", String.class, false);
		nodeTable.createListColumn("molecular_functions", String.class, false);
		nodeTable.createColumn("queried", String.class, false);
				
		for (UniProtEntry protein : interactorPool) {
			if (!nodeNameMap.containsKey(protein.getUniProtId())) {
				CyNode node = network.addNode();
				nodeNameMap.put(protein.getUniProtId(), node);
				
				CyRow nodeAttr = network.getRow(node);
				nodeAttr.set("name", protein.getUniProtId());
				nodeAttr.set("uniprot_id", protein.getUniProtId());
				nodeAttr.set("gene_name", protein.getGeneName());
				nodeAttr.set("ec_number", protein.getEcNumber());
				nodeAttr.set("synonym_gene_names", protein.getSynonymGeneNames());
				nodeAttr.set("protein_name", protein.getProteinName());
				nodeAttr.set("tax_id", String.valueOf(protein.getOrganism().getTaxId()));
				nodeAttr.set("reviewed", String.valueOf(protein.isReviewed()));
				nodeAttr.set("cellular_components_hidden", protein.getCellularComponentsAsStringList());
				nodeAttr.set("biological_processes_hidden", protein.getBiologicalProcessesAsStringList());
				nodeAttr.set("molecular_functions_hidden", protein.getMolecularFunctionsAsStringList());
				nodeAttr.set("orthologs", protein.getOrthologsAsStringList());
				nodeAttr.set("queried", String.valueOf(proteinOfInterestPool.contains(protein)));
				
				{
					List<String> cellularComponent = protein.getCellularComponentsAsStringList();
					List<String> cellularComponentReadable = new ArrayList<String>();
					if (cellularComponent != null && !cellularComponent.isEmpty())
						for (String s : cellularComponent) {
							JsonObject obj = JsonObject.readFrom(s);
							cellularComponentReadable.add(obj.get("term").asString());
						}
					nodeAttr.set("cellular_components", cellularComponentReadable);
					
					List<String> biologicalProcess = protein.getBiologicalProcessesAsStringList();
					List<String> biologicalProcessReadable = new ArrayList<String>();
					if (biologicalProcess != null && !biologicalProcess.isEmpty())
						for (String s : biologicalProcess) {
							JsonObject obj = JsonObject.readFrom(s);
							biologicalProcessReadable.add(obj.get("term").asString());
						}
					nodeAttr.set("biological_processes", biologicalProcessReadable);
					
					List<String> molecularFunction = protein.getMolecularFunctionsAsStringList();
					List<String> molecularFunctionReadable = new ArrayList<String>();
					if (molecularFunction != null && !molecularFunction.isEmpty())
						for (String s : molecularFunction) {
							JsonObject obj = JsonObject.readFrom(s);
							molecularFunctionReadable.add(obj.get("term").asString());
						}
					nodeAttr.set("molecular_functions", molecularFunctionReadable);
					
				}
				
				
				
			}
		}
	}

	private void createEdges(CyNetwork network) {
		// Edge attributes
		CyTable edgeTable = network.getDefaultEdgeTable();
		edgeTable.createColumn("Interactor_A", String.class, false);
		edgeTable.createColumn("Interactor_B", String.class, false);
		edgeTable.createColumn("Gene_name_A", String.class, false);
		edgeTable.createColumn("Gene_name_B", String.class, false);
		edgeTable.createColumn("Protein_name_A", String.class, false);
		edgeTable.createColumn("Protein_name_B", String.class, false);
		edgeTable.createListColumn("source", String.class, false);
		edgeTable.createListColumn("detmethod", String.class, false);
		edgeTable.createListColumn("type", String.class, false);
		edgeTable.createListColumn("interaction_id", String.class, false);
		edgeTable.createListColumn("pubid", String.class, false);
		edgeTable.createListColumn("confidence", String.class, false);
		edgeTable.createColumn("tax_id", String.class, false);
		edgeTable.createColumn("interolog", String.class, false);
		
		for(Organism organism: interactionsByOrg.keySet()) {
			boolean inRefOrg = (organism.equals(referenceOrganism));
			for(EncoreInteraction interaction: interactionsByOrg.get(organism)) {
				String nodeAName = "", nodeBName = "";
				CyNode nodeA = null, nodeB = null;
				if(inRefOrg) {
					nodeAName = interaction.getInteractorA("uniprotkb");
					nodeBName = interaction.getInteractorB("uniprotkb");
					nodeA = nodeNameMap.get(nodeAName);
					nodeB = nodeNameMap.get(nodeBName);					
				}
				else {
					for(UniProtEntry prot: interactorPool) {
						Protein ortho = prot.getOrtholog(organism);
						if(ortho != null) {
							if(interaction.getInteractorA().equals(ortho.getUniProtId())) {
								nodeAName = prot.getUniProtId();
								nodeA = nodeNameMap.get(nodeAName);
							}
							if(interaction.getInteractorB().equals(ortho.getUniProtId())) {
								nodeBName = prot.getUniProtId();
								nodeB = nodeNameMap.get(nodeBName);
							}
						}
						
						if(nodeA != null && nodeB != null) break;
					}
				}
				
				if(nodeA != null && nodeB != null) {				
					CyEdge myEdge = network.addEdge(nodeA, nodeB, true);
					
					CyTable nodeTable = network.getDefaultNodeTable();
					
					String geneNameA = "", geneNameB = "";
					String protNameA = "", protNameB = "";
					for (CyRow r : nodeTable.getAllRows()) {
						if (r.get("uniprot_id", String.class).equalsIgnoreCase(nodeAName)) {
							geneNameA = r.get("gene_name", String.class);
							protNameA = r.get("protein_name", String.class);
						}
						if (r.get("uniprot_id", String.class).equalsIgnoreCase(nodeBName)) {
							geneNameB = r.get("gene_name", String.class);
							protNameB = r.get("protein_name", String.class);
						}
					}
					
					
					CyRow edgeAttr = network.getRow(myEdge);
					edgeAttr.set("Interactor_A", nodeAName);
					edgeAttr.set("Interactor_B", nodeBName);
					edgeAttr.set("Gene_name_A", geneNameA);
					edgeAttr.set("Gene_name_B", geneNameB);
					edgeAttr.set("Protein_name_A", protNameA);
					edgeAttr.set("Protein_name_B", protNameB);
					edgeAttr.set("source", PsicquicResultTranslator.convert(interaction.getSourceDatabases()));
					edgeAttr.set("detmethod", PsicquicResultTranslator.convert(interaction.getMethodToPubmed().keySet()));
					edgeAttr.set("type", PsicquicResultTranslator.convert(interaction.getTypeToPubmed().keySet()));
					//edgeAttr.set("interaction_id", PsicquicResultTranslator.convert(interaction.getId()));
					edgeAttr.set("pubid", PsicquicResultTranslator.convert(interaction.getPublicationIds()));
					edgeAttr.set("confidence", PsicquicResultTranslator.convert(interaction.getConfidenceValues()));
					edgeAttr.set("tax_id", String.valueOf(organism.getTaxId()));
					edgeAttr.set("interolog", Boolean.toString(!inRefOrg));
				}
			}
		}
	}

	private CyNetworkView applyView(CyNetwork network) {
		if (network == null) {
			return null;
		}
		this.networkManager.addNetwork(network);

		final Collection<CyNetworkView> views = viewManager.getNetworkViews(network);
		CyNetworkView view = null;
		if (views.size() != 0) {
			view = views.iterator().next();
		}

		if (view == null) {
			// create a new view for my network
			view = viewFactory.createNetworkView(network);
			viewManager.addNetworkView(view);
		} else {
			System.out.println("networkView already existed.");
		}

		return view;
	}

	private void applyLayout(CyNetworkView view) {
		CyLayoutAlgorithm layout = layoutManager.getLayout("force-directed");
		Object context = layout.createLayoutContext();
		String layoutAttribute = null;
		insertTasksAfterCurrentTask(layout.createTaskIterator(view, context, CyLayoutAlgorithm.ALL_NODE_VIEWS, layoutAttribute));
	}

	private void applyVisualStyle(CyNetworkView view) {
		for (VisualStyle curVS : vizMapManager.getAllVisualStyles()) {
			if (curVS.getTitle().equalsIgnoreCase("PPiMapBuilder Visual Style")) {
				curVS.apply(view);
				vizMapManager.setCurrentVisualStyle(curVS);
				break;
			}
		}
		
		view.updateView();
	}
	
	public PMBInteractionNetworkBuildTaskFactory getPmbInteractionNetworkBuildTaskFactory() {
		return pmbInteractionNetworkBuildTaskFactory;
	}

}
