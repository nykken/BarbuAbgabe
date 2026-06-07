import { Client } from "@stomp/stompjs";

const host = import.meta.env.VITE_API_BASE_URL || "localhost:8080";

class StompService {
  private client: Client;
  isConnected: boolean = false;
  private statusListeners: Array<(connected: boolean) => void> = [];

  constructor() {
    this.client = new Client({
      brokerURL: `ws://${host}/ws`, //backend endpoint
      reconnectDelay: 5000, //if connection drops, try again after 5 seconds
      heartbeatOutgoing: 10000, //sends ping every 10 sec to server (to stay alive, server expects ping every 20 sec)
      heartbeatIncoming: 20000, //expects pong every 20 sec from server
    });

    // event listener
    this.client.onConnect = () => {
      this.isConnected = true;
      this.statusListeners.forEach((fn) => fn(true)); //notifies all hooks that connection is open
    };

    // event listener
    this.client.onDisconnect = () => {
      this.isConnected = false;
      this.statusListeners.forEach((fn) => fn(false)); //notifies all hooks that connection is closed
    };
  }

  // -- lifecycle methods --
  //starts the connection
  activate() {
    if (!this.client.active) {
      //prevents double activation
      this.client.activate();
    }
  }

  //closes the connection
  deactivate() {
    this.client.deactivate();
    this.isConnected = false;
    this.statusListeners.forEach((fn) => fn(false)); //updates ui
  }

  // -- communication methods --
  //listens to specific topic
  subscribe(path: string, callback: (payload: any) => void) {
    return this.client.subscribe(path, (message) => {
      //check if body exists to avoid JSON.parse(null) errors
      const data = message.body ? JSON.parse(message.body) : null;
      callback(data);
    });
  }

  //sends data to the backend
  send(path: string, payload?: any, headers?: Record<string, string>) {
    if (this.isConnected) {
      this.client.publish({
        destination: path,
        body: payload ? JSON.stringify(payload) : "",
        headers: headers,
      });
    }
  }

  //hooks-service bridge
  onStatusChange(callback: (connected: boolean) => void) {
    this.statusListeners.push(callback); //adds the hook's callback to the list
    callback(this.isConnected); //tells the hook the status immediately
    return () => {
      //returns the unsubscribe function to stop listening
      this.statusListeners = this.statusListeners.filter(
        (fn) => fn !== callback,
      );
    };
  }
}

export const stompService = new StompService();
