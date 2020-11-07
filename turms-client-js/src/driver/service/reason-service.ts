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

import {LoginFailureReason} from "../../model/login-failure-reason";
import {SessionDisconnectionReason} from "../../model/session-disconnection-reason";

// @ts-ignore
import fetch from "unfetch/dist/unfetch.es";
import StateStore from "../state-store";
import {im} from "../../model/proto-bundle";
import DeviceType = im.turms.proto.DeviceType;

/**
 * Fallback for the session close status in browsers
 * because the spec (https://html.spec.whatwg.org/multipage/web-sockets.html#feedback-from-the-protocol)
 * has required user agents to only convey 1006 in any situation for security purposes.
 */
export default class ReasonService {

    private static readonly DEFAULT_HTTP_URL = 'http://localhost:9510';

    private _stateStore: StateStore;
    private _url: string;

    constructor(stateStore: StateStore, url?: string) {
        this._stateStore = stateStore;
        this._url = url || ReasonService.DEFAULT_HTTP_URL;
    }

    queryLoginFailureReason(): Promise<LoginFailureReason> {
        const userId = this._stateStore.userInfo.userId;
        const deviceType = DeviceType[this._stateStore.userInfo.deviceType] || '';
        const requestId = this._stateStore.connectionRequestId;
        if (!userId || !requestId) {
            return Promise.reject(new Error('userId and requestId must not be null'));
        }
        const params = `userId=${userId}&deviceType=${deviceType}&requestId=${requestId}`;
        return fetch(`${this._url}/reasons/login-failure?${params}`)
            .catch((_: ProgressEvent) => {
                throw new Error(`Failed to fetch the reason for login failure`);
            })
            .then(response => {
                if (response.status === 200) {
                    return response.json();
                } else {
                    throw new Error(`Failed to fetch the reason for login failure: ${response.status}`);
                }
            });
    }

    queryDisconnectionReason(): Promise<SessionDisconnectionReason> {
        const userId = this._stateStore.userInfo.userId;
        const deviceType = DeviceType[this._stateStore.userInfo.deviceType] || '';
        const sessionId = this._stateStore.sessionId;
        if (!userId || !sessionId) {
            return Promise.reject(new Error('userId and sessionId must not be null'));
        }
        const params = `userId=${userId}&deviceType=${deviceType}&sessionId=${sessionId}`;
        return fetch(`${this._url}/reasons/disconnection?${params}`)
            .catch((_: ProgressEvent) => {
                throw new Error(`Failed to fetch the reason for session disconnection`);
            })
            .then(response => {
                if (response.status === 200) {
                    return response.json();
                } else {
                    throw new Error(`Failed to fetch the reason for session disconnection: ${response.status}`);
                }
            });
    }

}