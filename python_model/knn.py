from sklearn.neighbors import KNeighborsClassifier
import pandas as pd
import statistics


def knn(dataframes: list):
    cv_scores = []
    for i in range(len(dataframes)):
        train_dataframes = dataframes[:i] + dataframes[i + 1 :]

        model = KNeighborsClassifier(n_neighbors=5)

        train_df = pd.concat(train_dataframes)
        train_df.dropna(inplace=True)
        train_df = train_df.drop(columns=["timestamp"])

        X_train = train_df.drop(columns=["dnd_value"])
        y_train = train_df["dnd_value"]
        model.fit(X_train, y_train)

        test_df: pd.DataFrame = dataframes[i]
        test_df.dropna(inplace=True)
        test_df = test_df.drop(columns=['timestamp'])

        X_test = test_df.drop(columns=["dnd_value"])
        y_test = test_df["dnd_value"]

        cv_scores.append(model.score(X_test, y_test))

    print(
        f"KNN accuracy with leave-one-participant-out is {statistics.mean(cv_scores)}"
    )
