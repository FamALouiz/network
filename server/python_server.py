import socket
import threading
import pickle
from http.server import BaseHTTPRequestHandler, HTTPServer


#this is supposed to be our DB 
client_data = {}
data_lock = threading.Lock()



# TCP Server
def handle_client(client_socket, client_address):
    try:
        while True:
            data = client_socket.recv(1024)
            if not data:
                break

            resource_info = pickle.loads(data)
            with data_lock:
                client_data[client_address] = resource_info
                print(f"Updated data from {client_address}: {resource_info}")

            client_socket.send(b"Data received.")
    except Exception as e:
        print(f"Error with client {client_address}: {e}")
    finally:
        client_socket.close()

def start_tcp_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', 3030))
    server_socket.listen(5)
    print("TCP Server listening on port 3030...")

    while True:
        client_socket, client_address = server_socket.accept()
        print(f"New connection from {client_address}")
        threading.Thread(target=handle_client, args=(client_socket, client_address), daemon=True).start()


