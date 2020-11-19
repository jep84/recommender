package book.recommendation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

public class App {
	public static void main(String[] args) throws IOException, TasteException {
		// DataModel model = new FileDataModel(new File("./data/dataset.csv"));

		DataModel model = new GenericDataModel(getUserData(args[0]));

		UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
		UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
		UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);

		// solicita 3 recomendações para o userId "2"
		List<RecommendedItem> recommendations = recommender.recommend(2, 3);
		
		System.out.println("--------------- RECOMMENDATION ---------------");
		
		for (RecommendedItem recommenndation : recommendations)
			System.out.println(recommenndation);
		
		System.out.println("--------------- EVALUATION ---------------");

		evaluation(args[0]);
	}

	public static void evaluation(String filePath) throws TasteException {
		DataModel model = new GenericDataModel(getUserData(filePath));
		RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
		RecommenderBuilder builder = new RecommenderBuilder() {
			public Recommender buildRecommender(DataModel dataModel) throws TasteException {
				UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
				UserNeighborhood neighborhood = new NearestNUserNeighborhood(2, similarity, dataModel);
				
				// recomenda com base no usuario
				return new GenericUserBasedRecommender(dataModel, neighborhood, similarity);

				// recomenda com base no item
				// https://mahout.apache.org/docs/0.13.0/api/docs/mahout-mr/org/apache/mahout/cf/taste/impl/recommender/GenericItemBasedRecommender.html
			}
		};
		double result = evaluator.evaluate(builder, null, model, 0.9, 1.0);
		System.out.println(result);
	}

	/**
	 * Returns user data for recommendations NOTE: This should not be used as
	 * production code.
	 * 
	 * @return
	 */
	private static FastByIDMap<PreferenceArray> getUserData(String dataSetFile) {
		FastByIDMap<PreferenceArray> preferences = new FastByIDMap<PreferenceArray>();
		PreferenceArray userPreferences = new GenericUserPreferenceArray(31);

		String csvFile = dataSetFile;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		int lastPurchaser = 1;
		int i = 0;
		int currentPurchaserId = 0;

		try {
			br = new BufferedReader(new FileReader(csvFile));

			while ((line = br.readLine()) != null) {
				String[] data = line.split(cvsSplitBy);

				currentPurchaserId = Integer.parseInt(data[0]);

				if (lastPurchaser != currentPurchaserId) {
					preferences.put(lastPurchaser, userPreferences);

					userPreferences = new GenericUserPreferenceArray(31);
					currentPurchaserId = Integer.parseInt(data[0]);
					i = 0;
				}
				userPreferences.setUserID(i, Integer.parseInt(data[0]));
				userPreferences.setItemID(i, Integer.parseInt(data[1]));
				userPreferences.setValue(i, (float) Integer.parseInt(data[2]));

				lastPurchaser = Integer.parseInt(data[0]);

				i++;
			}

			preferences.put(currentPurchaserId, userPreferences);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return preferences;
	}
}
