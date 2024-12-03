from socket import socket, AF_INET, SOCK_STREAM
import time
from data_reader import DataReader
import pickle


class Client:
    def __init__(self, port: int, max_retry: int = 5, retry_interval: int = 2):
        self.client: socket = socket(AF_INET, SOCK_STREAM)
        self.port: int = port
        self.max_retry: int = max_retry
        self.retry_interval: int = retry_interval
        self.data_reader = DataReader()

    def connect(self, host: str) -> bool:
        for i in range(self.max_retry):
            try:
                self.client.connect((host, self.port))
                print(f"Connected to {host}:{self.port}")
                return True
            except ConnectionRefusedError:
                print(
                    f"Connection refused. Retry {i + 1}... retrying in {self.retry_interval} seconds.")
                time.sleep(self.retry_interval)
        return False

    def send(self, data: str = None) -> bool:
        try:
            data_to_send = data.encode(
                'utf-8') or pickle.dumps(self.data_reader.get_pc_data())
            print(f"Sending data: {data_to_send}")
            self.client.send(
                data_to_send
            )
            return True
        except Exception as e:
            print(f"Error: {e}")
            return False

    def close(self):
        try:
            self.client.close()
            return True
        except Exception as e:
            print(f"Error: {e}")
            return False

    def recieve(self):
        data = self.client.recv(1024)
        print(f"Received data: {data}")


def main():
    client = Client(3030)
    if client.connect('localhost'):
        while True:
            if not client.send("Hello from client"):
                break
            time.sleep(1)
    client.close()


if __name__ == '__main__':
    main()
