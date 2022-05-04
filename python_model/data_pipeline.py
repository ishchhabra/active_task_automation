from typing import Tuple
import pandas
import numpy as np


DATA_FOLDER_PATH = "data"
GPS_FILE_NAME = "gps_data.csv"
DND_FILE_NAME = "dnd_data.csv"
GPS_WINDOW_SIZE = 5000  # in milliseconds


class DataPipeline:
    def __init__(self: "DataPipeline", participant: str):
        dnd_data = self.load_dnd(participant)
        gps_data = self.load_gps(participant)

        self.dataframe = self.time_synchronize(dnd_data, gps_data)

    @staticmethod
    def load_dnd(participant: str):
        dataframe = pandas.read_csv(
            f"{DATA_FOLDER_PATH}/{participant}/{DND_FILE_NAME}", sep=";"
        )
        dataframe.columns = ["timestamp", "dnd_value"]
        return dataframe

    @staticmethod
    def load_gps(participant: str):
        dataframe = pandas.read_csv(
            f"{DATA_FOLDER_PATH}/{participant}/{GPS_FILE_NAME}", sep=";"
        )
        dataframe.columns = ["timestamp", "latitude", "longitude"]
        return dataframe

    @staticmethod
    def time_synchronize(dnd_data: pandas.DataFrame, gps_data: pandas.DataFrame):
        def gps_windowed_average(row) -> pandas.Series:
            timestamp = row['timestamp']

            rows = gps_data.loc[
                (gps_data["timestamp"] >= timestamp - GPS_WINDOW_SIZE)
                & (gps_data["timestamp"] <= timestamp + GPS_WINDOW_SIZE)
            ]

            avg = rows.mean(axis=0)
            return pandas.Series([avg['latitude'], avg['longitude']])

        result_df = dnd_data.copy()
        result_df[['latitude', 'longitude']] = result_df.apply(gps_windowed_average, axis=1)
        return result_df
