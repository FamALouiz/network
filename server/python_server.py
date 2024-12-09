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

# Web Server
class WebServer(BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        
        
        
        # OKOK We still need to edit this part for the colors thing.
        
        with data_lock:
            html = "<html><head><title>Resource Info</title></head><body>"
            html += "<h1>Resource Information</h1>"
            for client, resources in client_data.items():
                html += f"<h2>Client {client}</h2><pre>{resources}</pre>"
            html += "</body></html>"

        self.wfile.write(html.encode('utf-8'))

def start_web_server():
    http_server = HTTPServer(('0.0.0.0', 8080), WebServer)
    print("Web Server running on port 8080...")
    http_server.serve_forever()




if __name__ == "__main__":
    tcp_thread = threading.Thread(target=start_tcp_server, daemon=True)
    tcp_thread.start()

    start_web_server()
