from time import time
import pandas
import numpy as np

DATA_FOLDER_PATH = "data"
GPS_FILE_NAME = "gps_data.csv"
DND_FILE_NAME = "dnd_data.csv"
GPS_WINDOW_SIZE = 5000 # in milliseconds

class DataPipeline:
    def __init__(self: "DataPipeline", participant: str):
        dnd_data = self.load_dnd(participant)
        gps_data = self.load_gps(participant)

        self.data = self.time_synchronize(dnd_data, gps_data)

    @staticmethod
    def load_dnd(participant: str):
        dataframe = pandas.read_csv(f"{DATA_FOLDER_PATH}/{participant}/{DND_FILE_NAME}", sep=";")
        dataframe.columns = ["timestamp", "dnd_value"]
        return dataframe

    @staticmethod
    def load_gps(participant: str):
        dataframe = pandas.read_csv(f"{DATA_FOLDER_PATH}/{participant}/{GPS_FILE_NAME}", sep=";")
        dataframe.columns = ["timestamp", "latitude", "longitude"]
        return dataframe
    
    @staticmethod
    def time_synchronize(dnd_data: pandas.DataFrame, gps_data: pandas.DataFrame):
        # Given two dataframes, take a window of length 2 * GPS_WINDOW_SIZE from [dnd_timestamp - GPS_WINDOW_SIZE, dnd_timestamp + GPS_WINDOW_SIZE]. 
        # Find all the gps rows with timestamp in this range, and then average over axis=0.
        # Do this for all the rows in DND dataframe, and return new dataframe which has ['timestamp' (from dnd), 'dnd_value', 'latitude', 'longitude']

        def dnd_windowed_average(timestamp: int): 
            averages = []
            start = timestamp[0] + GPS_WINDOW_SIZE
            for i in range(start ,len(timestamp)-GPS_WINDOW_SIZE,2*GPS_WINDOW_SIZE):
                window = timestamp[i-GPS_WINDOW_SIZE:i+GPS_WINDOW_SIZE]
                averages.append(np.average(window, axis=0))
            return averages

        for i in range(dnd_data.size):
            dnd_windowed_average(dnd_data["timestamp"].to_numpy(int))

        return dnd_data.merge(gps_data, on="timestamp")