import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.item.ExecutionContext;


public class TestMain {

	
	public  static void main(String[] args) {
		
		int gridSize = 2;
		List<Long> idsFromTemporaryTable = new ArrayList<Long>();
		idsFromTemporaryTable.add(1l);
		idsFromTemporaryTable.add(2l);
		idsFromTemporaryTable.add(3l);
		
		Map<String, ExecutionContext> executionContextMap = new HashMap<String, ExecutionContext>();
		
		
		int count = 0;
		int numberOfPartition = 0;
		ExecutionContext singleExecutionContext = null;
		List<Long> listID = new ArrayList<Long>();

		for (Long id : idsFromTemporaryTable) {
			listID.add(id);
			count++;
			if (count % gridSize == 0 && count != 0) {
				singleExecutionContext = new ExecutionContext();
				singleExecutionContext.put("ID", listID);
				executionContextMap.put("Partition#" + numberOfPartition, singleExecutionContext);
				numberOfPartition++;
				listID = new ArrayList<Long>();
			}
		}
		if (listID != null && listID.size() > 0) {
			singleExecutionContext = new ExecutionContext();
			singleExecutionContext.put("ID", listID);
			executionContextMap.put("Partition#" + numberOfPartition, singleExecutionContext);
			numberOfPartition++;
			listID = new ArrayList<Long>();
		}
		
		
		
	}
}
