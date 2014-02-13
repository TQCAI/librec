package librec.core;

import librec.data.DenseMatrix;
import librec.data.SparseMatrix;
import librec.data.SparseVector;
import librec.intf.IterativeRecommender;

/**
 * Ruslan Salakhutdinov and Andriy Mnih, <strong>Probabilistic Matrix
 * Factorization</strong>, NIPS 2008. <br/>
 * 
 * @author guoguibing
 * 
 */
public class PMF extends IterativeRecommender {

	protected DenseMatrix userDeltas, itemDeltas;

	public PMF(SparseMatrix rm, SparseMatrix tm, int fold) {
		super(rm, tm, fold);

		algoName = "PMF";

		// disable bold driver
		isBoldDriver = false;
	}

	@Override
	protected void initModel() {
		super.initModel();

		userDeltas = new DenseMatrix(numUsers, numFactors);
		itemDeltas = new DenseMatrix(numItems, numFactors);
	}

	@Override
	protected void buildModel() {

		// batch updates with momentums
		for (int iter = 1; iter <= maxIters; iter++) {

			DenseMatrix userSgds = new DenseMatrix(numUsers, numFactors);
			DenseMatrix itemSgds = new DenseMatrix(numItems, numFactors);
			loss = 0;
			errs = 0;

			for (int u = 0; u < numUsers; u++) {

				SparseVector uv = trainMatrix.row(u);
				int[] items = uv.getIndex();

				for (int j : items) {
					double rate = uv.get(j);
					double pred = predict(u, j);
					double euj = rate - pred;
					loss += euj * euj;
					errs += euj * euj;

					for (int f = 0; f < numFactors; f++) {
						double qjf = Q.get(j, f);
						double puf = P.get(u, f);

						double sgd_u = 2 * euj * qjf - regU * puf;
						double sgd_j = 2 * euj * puf - regI * qjf;

						userSgds.add(u, f, sgd_u);
						itemSgds.add(j, f, sgd_j);

						loss += regU * puf * puf + regI * qjf * qjf;
					}
				}
			}
			errs /= numRates;
			loss /= numRates;

			userDeltas.scale(momentum).add(userSgds.scale(lRate / numRates));
			itemDeltas.scale(momentum).add(itemSgds.scale(lRate / numRates));

			P.add(userDeltas);
			Q.add(itemDeltas);

			if (isConverged(iter))
				break;
		}
	}

}
