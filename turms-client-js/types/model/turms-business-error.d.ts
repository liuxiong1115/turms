import { im } from "./proto-bundle";
import TurmsNotification = im.turms.proto.TurmsNotification;
export default class TurmsBusinessError extends Error {
    private readonly _code;
    private readonly _reason;
    private constructor();
    get code(): number;
    get reason(): string;
    toString(): string;
    static fromNotification(notification: TurmsNotification): TurmsBusinessError;
    static from(code: number, reason?: string): TurmsBusinessError;
    static fromCode(code: number): TurmsBusinessError;
    static illegalParam<T = never>(reason: string): Promise<T>;
    static notFalsy<T = never>(name: string, notEmpty?: boolean): Promise<T>;
}
