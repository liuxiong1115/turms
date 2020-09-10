export interface SessionDisconnectInfo {
    code?: number;
    webSocketStatusCode?: number;
    webSocketReason?: string;
    error?: Error;
}
