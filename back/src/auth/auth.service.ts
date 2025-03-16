// src/auth/auth.service.ts
import { EntityManager } from '@mikro-orm/postgresql';
import { Injectable, UnauthorizedException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { User } from '~/db/entities/user.entity';
import { JwtPayload } from './jwt/jwt.domain';

@Injectable()
export class AuthService {
    constructor(
        private readonly em: EntityManager,
        private readonly jwtService: JwtService,
    ) {}

    async register(email: string, password: string): Promise<User> {
        const hashedPassword = await bcrypt.hash(password, 10);
        const user = this.em.create(User, {
            email,
            password: hashedPassword,
        });
        await this.em.persistAndFlush(user);
        return user;
    }

    async login(
        email: string,
        password: string,
    ): Promise<{ access_token: string }> {
        const user = await this.em.findOne(User, { email });
        if (!user) throw new UnauthorizedException('Invalid credentials');

        const passwordValid = await bcrypt.compare(password, user.password);
        if (!passwordValid)
            throw new UnauthorizedException('Invalid credentials');

        const payload: JwtPayload = { sub: user.id, email: user.email };
        const token = this.jwtService.sign(payload);

        return { access_token: token };
    }
}
