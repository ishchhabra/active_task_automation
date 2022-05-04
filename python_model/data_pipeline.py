from tkinter import dnd
from typing import Tuple
import pandas as pd
import numpy as np
import os


DATA_FOLDER_PATH = "data"
GPS_FILE_NAME = "gps_data.csv"
DND_FILE_NAME = "dnd_data.csv"
BAROMETER_FILE_NAME = "barometer_data.csv"
ACCELEROMETER_FILE_NAME = "accelerometer_data.csv"
GPS_WINDOW_SIZE = 5000  # in milliseconds


class DataPipeline:
    def __init__(
        self: "DataPipeline",
        participant: str,
        use_barometer: bool = False,
        use_accelerometer: bool = False,
    ):
        if os.path.exists(f"{DATA_FOLDER_PATH}/{participant}/cached_data.csv"):
            self.dataframe = pd.read_csv(
                f"{DATA_FOLDER_PATH}/{participant}/cached_data.csv", sep=";"
            )
        else:
            dnd_data = self.load_dnd(participant)
            gps_data = self.load_gps(participant)

            barometer_data = None
            if use_barometer is True:
                barometer_data = self.load_barometer(participant)

            if use_accelerometer is True:
                accelerometer_data = self.load_accelerometer(participant)

            self.dataframe = self.time_synchronize(
                dnd_data, gps_data, barometer_data, accelerometer_data
            )

            self.dataframe.to_csv(
                f"{DATA_FOLDER_PATH}/{participant}/cached_data.csv", sep=";"
            )

    @staticmethod
    def load_dnd(participant: str):
        dataframe = pd.read_csv(
            f"{DATA_FOLDER_PATH}/{participant}/{DND_FILE_NAME}", sep=";"
        )
        dataframe.columns = ["timestamp", "dnd_value"]
        return dataframe

    @staticmethod
    def load_gps(participant: str):
        dataframe = pd.read_csv(
            f"{DATA_FOLDER_PATH}/{participant}/{GPS_FILE_NAME}", sep=";"
        )
        dataframe.columns = ["timestamp", "latitude", "longitude"]
        return dataframe

    @staticmethod
    def load_barometer(participant: str):
        dataframe = pd.read_csv(
            f"{DATA_FOLDER_PATH}/{participant}/{BAROMETER_FILE_NAME}", sep=";"
        )
        dataframe.columns = ["timestamp", "barometer"]
        dataframe["timestamp"] = dataframe["timestamp"]
        return dataframe

    @staticmethod
    def load_accelerometer(participant: str):
        dataframe = pd.read_csv(
            f"{DATA_FOLDER_PATH}/{participant}/{ACCELEROMETER_FILE_NAME}", sep=";"
        )
        dataframe.columns = [
            "timestamp",
            "accelerometer_x",
            "accelerometer_y",
            "accelerometer_z",
        ]
        dataframe["timestamp"] = dataframe["timestamp"]
        return dataframe

    @staticmethod
    def time_synchronize(
        dnd_data: pd.DataFrame,
        gps_data: pd.DataFrame,
        barometer_data: pd.DataFrame = None,
        accelerometer_data: pd.DataFrame = None,
    ):
        # Minor hack on sensors' timestamps since it wasn't recorded correctly
        if barometer_data is not None:
            barometer_data["timestamp"] = barometer_data["timestamp"] / 1000
            barometer_data["timestamp"] = barometer_data["timestamp"] - (
                barometer_data["timestamp"][0] - dnd_data["timestamp"][0]
            )

        if accelerometer_data is not None:
            accelerometer_data["timestamp"] = accelerometer_data["timestamp"] / 1000
            accelerometer_data["timestamp"] = accelerometer_data["timestamp"] - (
                accelerometer_data["timestamp"][0] - dnd_data["timestamp"]
            )

        def accelerometer_windowed_averge(row) -> pd.DataFrame:
            timestamp = row["timestamp"]

            rows = accelerometer_data.loc[
                (accelerometer_data["timestamp"] >= timestamp - GPS_WINDOW_SIZE)
                & (accelerometer_data["timestamp"] <= timestamp + GPS_WINDOW_SIZE)
            ]

            avg = rows.mean(axis=0)

            return np.sqrt(
                (
                    np.square((avg["accelerometer_x"]))
                    + np.square((avg["accelerometer_y"]))
                    + np.square((avg["accelerometer_z"]))
                )
            )

        def barometer_windowed_average(row) -> pd.DataFrame:
            timestamp = row["timestamp"]

            rows = barometer_data.loc[
                (barometer_data["timestamp"] >= timestamp - GPS_WINDOW_SIZE)
                & (barometer_data["timestamp"] <= timestamp + GPS_WINDOW_SIZE)
            ]

            avg = rows.mean(axis=0)
            return avg["barometer"]

        def gps_windowed_average(row) -> pd.Series:
            timestamp = row["timestamp"]

            rows = gps_data.loc[
                (gps_data["timestamp"] >= timestamp - GPS_WINDOW_SIZE)
                & (gps_data["timestamp"] <= timestamp + GPS_WINDOW_SIZE)
            ]

            avg = rows.mean(axis=0)
            return pd.Series([avg["latitude"], avg["longitude"]])

        result_df = dnd_data.copy()
        result_df[["latitude", "longitude"]] = result_df.apply(
            gps_windowed_average, axis=1
        )

        if barometer_data is not None:
            result_df["barometer_mean"] = result_df.apply(
                barometer_windowed_average, axis=1
            )

        if accelerometer_data is not None:
            result_df["accelerometer_mean"] = result_df.apply(
                accelerometer_windowed_averge, axis=1
            )

        return result_df
