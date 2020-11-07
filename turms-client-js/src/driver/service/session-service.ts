/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {SessionDisconnectInfo} from "../../model/session-disconnect-info";
import StateStore from "../state-store";
import TurmsCloseStatus from "../../model/turms-close-status";

export enum SessionStatus {
    CONNECTED,
    DISCONNECTED,
    CLOSED
}

export default class SessionService {

    private _stateStore: StateStore;

    private _currentStatus: SessionStatus = SessionStatus.CLOSED;

    private _onSessionConnectedListeners: (() => void)[] = [];
    private _onSessionDisconnectedListeners: ((disconnectInfo: SessionDisconnectInfo) => void)[] = [];
    private _onSessionClosedListeners: ((disconnectInfo: SessionDisconnectInfo) => void)[] = [];

    constructor(stateStore: StateStore) {
        this._stateStore = stateStore;
    }

    set sessionId(sessionId: string) {
        this._stateStore.sessionId = sessionId;
    }

    // Status

    getStatus(): SessionStatus {
        return this._currentStatus;
    }

    isConnected(): boolean {
        return this._currentStatus === SessionStatus.CONNECTED;
    }

    isClosed(): boolean {
        return this._currentStatus === SessionStatus.CLOSED;
    }

    // Listeners

    addOnSessionConnectedListener(listener: () => void): void {
        this._onSessionConnectedListeners.push(listener);
    }

    addOnSessionDisconnectedListener(listener: (disconnectInfo: SessionDisconnectInfo) => void): void {
        this._onSessionDisconnectedListeners.push(listener);
    }

    addOnSessionClosedListener(listener: (disconnectInfo: SessionDisconnectInfo) => void): void {
        this._onSessionClosedListeners.push(listener);
    }

    notifyOnSessionConnectedListeners(): void {
        if (this._currentStatus == SessionStatus.DISCONNECTED || this._currentStatus == SessionStatus.CLOSED) {
            for (const cb of this._onSessionConnectedListeners) {
                cb.call(this);
            }
            this._currentStatus = SessionStatus.CONNECTED;
        }
    }

    notifyOnSessionDisconnectedListeners(event: CloseEvent, info: SessionDisconnectInfo): void {
        const disconnectInfo = SessionService._parseDisconnectInfo(event, info);
        if (this._currentStatus == SessionStatus.CONNECTED) {
            for (const cb of this._onSessionDisconnectedListeners) {
                cb.call(this, disconnectInfo);
            }
            this._currentStatus = SessionStatus.DISCONNECTED;
        }
    }

    notifyOnSessionClosedListeners(event: CloseEvent, info: SessionDisconnectInfo): void {
        switch (this._currentStatus) {
            case SessionStatus.CONNECTED:
                this.notifyOnSessionDisconnectedListeners(event, info);
                break;
            case SessionStatus.DISCONNECTED:
                this._currentStatus = SessionStatus.DISCONNECTED;
                break;
            case SessionStatus.CLOSED:
                return;
        }
        const disconnectInfo = SessionService._parseDisconnectInfo(event, info);
        for (const cb of this._onSessionClosedListeners) {
            cb.call(this, disconnectInfo);
        }
        this._currentStatus = SessionStatus.CLOSED;
    }

    // Parsers

    private static _parseDisconnectInfo(event: CloseEvent, info: SessionDisconnectInfo): SessionDisconnectInfo {
        const code = TurmsCloseStatus[event.code] ? event.code : null;
        return {
            ...info,
            closeStatus: code,
            webSocketStatusCode: event.code,
            webSocketReason: event.reason,
        };
    }

}