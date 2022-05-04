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

        self.data = self.time_synchronize(dnd_data, gps_data)

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
        # Given two dataframes, take a window of length 2 * GPS_WINDOW_SIZE from [dnd_timestamp - GPS_WINDOW_SIZE, dnd_timestamp + GPS_WINDOW_SIZE].
        # Find all the gps rows with timestamp in this range, and then average over axis=0.
        # Do this for all the rows in DND dataframe, and return new dataframe which has ['timestamp' (from dnd), 'dnd_value', 'latitude', 'longitude']

        # dnd_data_np = dnd_data.to_numpy()
        # gps_data_np = dnd_data.to_numpy()

        def gps_windowed_average(timestamp: int) -> Tuple[int, int]:
            rows = gps_data.loc[
                (gps_data["timestamp"] >= timestamp - GPS_WINDOW_SIZE)
                & (gps_data["timestamp"] <= timestamp + GPS_WINDOW_SIZE)
            ]

            return rows.mean(axis=0)

        for i in range(dnd_data.size):
            gps_windowed_average(dnd_data["timestamp"][i])

        return dnd_data.merge(gps_data, on="timestamp")
