export interface SessionDisconnectInfo {
    wasConnected: boolean;
    isClosedByClient: boolean;
    isReconnecting: boolean;
    closeStatus?: number;
    webSocketStatusCode?: number;
    webSocketReason?: string;
    error?: Error;
}
