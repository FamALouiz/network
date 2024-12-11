from flask import Flask, render_template, redirect, request
from socket import socket, AF_INET, SOCK_STREAM
import time

app = Flask(__name__)


class Interface:
    def __init__(self, port: int = 4040, max_retry: int = 5, retry_interval: int = 2):
        self.client: socket = socket(AF_INET, SOCK_STREAM)
        self.port: int = port
        self.max_retry: int = max_retry
        self.retry_interval = retry_interval

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

    def close(self):
        try:
            self.client.close()
            return True
        except Exception as e:
            print(f"Error: {e}")
            return False

    def recieve(self):
        data = self.client.recv(1024).decode('utf-8')
        print(f"Received data: {data}")
        return data

    def send(self, client, data_cpu, data_ram, data_gpu) -> bool:
        set_thresh = "SET_THRESHOLD"
        try:
            data_str = f"client:{client} cpu:{data_cpu} ram:{data_ram} gpu:{data_gpu}\n"
            self.client.send(
                data_str.encode('utf-8')
            )
            return True
        except Exception as e:
            print(f"Error: {e}")
            return False


interface = Interface()
interface.connect("localhost")


@app.route("/", methods=["GET"])
def home():
    return render_template("index.html")


@app.route("/get_data", methods=["GET"])
def get_data():
    data = interface.recieve()
    client = eval(data.split("Client")[1].split(" ")[1])
    cpu = eval(data.split("CPU:")[1].split(" ")[1])
    ram = eval(data.split("RAM:")[1].split(" ")[1])
    gpu = eval(data.split("GPU:")[1].split(" ")[1])
    alert = True if data.split("Alert:")[1].strip() == "true" else False

    return {
        "client": client,
        "cpu": cpu,
        "ram": ram,
        "gpu": gpu,
        "alert": alert
    }


@app.route("/update_thresh", methods=["POST"])
def update_thresh():
    if (request.method == "POST"):
        data = request.form
        interface.send(data.get("client"), data.get("cpu-threshold"),
                       data.get("ram-threshold"), data.get("gpu-threshold"))
        print(f"Received threshold update data: {data}")
        return redirect("/")


if __name__ == "__main__":
    app.run("0.0.0.0", port=1234, debug=True, threaded=True)
