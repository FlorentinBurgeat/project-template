import { Entity, PrimaryKey, Property, DateType } from '@mikro-orm/core';

@Entity()
export class User {
    @PrimaryKey()
    id!: number;

    @Property({ unique: true })
    email!: string;

    @Property()
    password!: string; // Stocker les mots de passe hashés

    @Property({ type: DateType, onCreate: () => new Date() })
    createdAt?: Date;
}
