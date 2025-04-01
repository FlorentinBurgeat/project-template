import { IsEmail, IsNotEmpty, IsString, MinLength } from 'class-validator';

export class RegisterDto {
    @IsEmail({}, { message: 'Veuillez fournir un email valide' })
    @IsNotEmpty({ message: "L'email est requis" })
    email: string;

    @IsString({ message: 'Le mot de passe doit être une chaîne de caractères' })
    @IsNotEmpty({ message: 'Le mot de passe est requis' })
    @MinLength(8, {
        message: 'Le mot de passe doit contenir au moins 8 caractères',
    })
    password: string;
}
