export interface JwtPayload {
    sub: number; // ID de l'utilisateur
    email: string;
    iat?: number; // Date d'émission du token
    exp?: number; // Date d'expiration du token
}
