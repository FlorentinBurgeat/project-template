import {
    Injectable,
    UnauthorizedException,
    ConflictException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { EntityManager } from '@mikro-orm/core';
import * as bcrypt from 'bcrypt';
import { User } from '../../db/entities/user.entity';
import { JwtPayload } from '../interfaces';
import { LoginDto, RegisterDto } from '../dto';

@Injectable()
export class AuthService {
    constructor(
        private readonly em: EntityManager,
        private readonly jwtService: JwtService,
    ) {}

    async register(registerDto: RegisterDto): Promise<{ accessToken: string }> {
        const { email, password } = registerDto;

        // Vérifier si l'utilisateur existe déjà
        const existingUser = await this.em.findOne(User, { email });
        if (existingUser) {
            throw new ConflictException('Cet email est déjà utilisé');
        }

        // Hasher le mot de passe
        const hashedPassword = await bcrypt.hash(password, 10);

        // Créer un nouvel utilisateur
        const user = new User();
        user.email = email;
        user.password = hashedPassword;

        // Sauvegarder l'utilisateur dans la base de données
        await this.em.persistAndFlush(user);

        // Générer un token JWT
        return this.generateToken(user);
    }

    async login(loginDto: LoginDto): Promise<{ accessToken: string }> {
        const { email, password } = loginDto;

        // Trouver l'utilisateur par email
        const user = await this.em.findOne(User, { email });
        if (!user) {
            throw new UnauthorizedException('Email ou mot de passe incorrect');
        }

        // Vérifier le mot de passe
        const isPasswordValid = await bcrypt.compare(password, user.password);
        if (!isPasswordValid) {
            throw new UnauthorizedException('Email ou mot de passe incorrect');
        }

        // Générer un token JWT
        return this.generateToken(user);
    }

    private generateToken(user: User): { accessToken: string } {
        const payload: JwtPayload = {
            sub: user.id,
            email: user.email,
        };

        return {
            accessToken: this.jwtService.sign(payload),
        };
    }

    async validateUser(payload: JwtPayload): Promise<User | null> {
        return this.em.findOne(User, { id: payload.sub });
    }
}
