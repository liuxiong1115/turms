enum Code {
    // For general use
    NO_CONTENT = 2001,

    // Used by client only
    USER_ID_AND_PASSWORD_MUST_NOT_NULL = 8100,
    SESSION_HAS_BEEN_CLOSED = 8200,
    SESSION_ALREADY_ESTABLISHED,
    REQUESTS_TOO_FREQUENT = 8300,
    REQUEST_TIMEOUT,
    INVALID_RESPONSE = 8400
}

const code2ReasonMap = {
    [Code.USER_ID_AND_PASSWORD_MUST_NOT_NULL]: "The user ID and password must not be null",
    [Code.SESSION_HAS_BEEN_CLOSED]: "The session has been closed",
    [Code.SESSION_ALREADY_ESTABLISHED]: "The session has been established",
    [Code.REQUESTS_TOO_FREQUENT]: "Requests are too frequent",
    [Code.INVALID_RESPONSE]: "The response is invalid",
    [Code.REQUEST_TIMEOUT]: "The request has timed out"
};

class TurmsStatusCode {
    private code: number;
    private reason: string;

    constructor(code: number) {
        this.code = code;
        this.reason = TurmsStatusCode.getReason(code);
    }

    static isSuccessCode(code: number | string | Code): boolean {
        if (typeof code === 'string') {
            code = parseInt(code);
        }
        return 2000 <= code && code < 3000;
    }

    static isErrorCode(code: number | string): boolean {
        return !this.isSuccessCode(code);
    }

    static getReason(code: number): string {
        return code2ReasonMap[code];
    }
}

export default Object.assign(TurmsStatusCode, Code);