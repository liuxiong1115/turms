export interface SessionDisconnectInfo {
    closeStatus?: number;
    webSocketStatusCode?: number;
    webSocketReason?: string;
    error?: Error;
}
