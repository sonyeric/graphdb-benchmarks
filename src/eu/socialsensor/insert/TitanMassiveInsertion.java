package eu.socialsensor.insert;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.batch.BatchGraph;
import com.tinkerpop.blueprints.util.wrappers.batch.VertexIDType;

public class TitanMassiveInsertion implements Insertion {
	
	private TitanGraph titanGraph = null;
	private BatchGraph<TitanGraph> batchGraph = null;
	
	public static void main(String args[]) {
		TitanMassiveInsertion test = new TitanMassiveInsertion();
		test.startup("data/titanDB");
		test.createGraph("data/amazonEdges.txt");
		test.shutdown();		
	}
	
	/**
	 * Start the titan database and configure for massive insertion
	 * @param titanDBDir
	 */
	public void startup(String titanDBDir) {
		System.out.println("The Titan database is now starting . . . .");
		BaseConfiguration config = new BaseConfiguration();
        Configuration storage = config.subset(GraphDatabaseConfiguration.STORAGE_NAMESPACE);
        storage.setProperty(GraphDatabaseConfiguration.STORAGE_BACKEND_KEY, "local");
        storage.setProperty(GraphDatabaseConfiguration.STORAGE_DIRECTORY_KEY, titanDBDir);
        storage.setProperty(GraphDatabaseConfiguration.STORAGE_BATCH_KEY, true);
        titanGraph = TitanFactory.open(config);
		titanGraph.makeKey("nodeId").dataType(String.class).indexed(Vertex.class).make();
		titanGraph.makeLabel("similar").unidirected().make();
		titanGraph.commit();
		batchGraph = new BatchGraph<TitanGraph>(titanGraph, VertexIDType.STRING, 10000);
		batchGraph.setVertexIdKey("nodeId");
		batchGraph.setLoadingFromScratch(true);
		
	}
	
	public void shutdown() {
		System.out.println("The Titan database is now shuting down . . . .");
		if(titanGraph != null) {
			batchGraph.shutdown();
			titanGraph.shutdown();
			batchGraph = null;
			titanGraph = null;
		}
	}
	
	public void createGraph(String datasetDir) {
		System.out.println("Creating the Titan database . . . .");		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datasetDir)));
			String line;
			int lineCounter = 1;
			Vertex srcVertex, dstVertex;
			while((line = reader.readLine()) != null) {
				if(lineCounter > 4) {
					String[] parts = line.split("\t");
					
					srcVertex = getOrCreate(parts[0]);
					dstVertex = getOrCreate(parts[1]);
					
					srcVertex.addEdge("similar", dstVertex);
				}
				lineCounter++;
			}
			reader.close();
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private Vertex getOrCreate(String value) {
		Vertex vertex = batchGraph.getVertex(value);
		if(vertex == null) {
			vertex = batchGraph.addVertex(value);
			vertex.setProperty("nodeId", value);
		}
		return vertex;
		
	}


}