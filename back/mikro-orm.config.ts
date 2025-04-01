import { defineConfig } from '@mikro-orm/postgresql';
import { User } from './src/db/entities/user.entity';
import { Migrator } from '@mikro-orm/migrations';
import { config } from 'dotenv';
config();

export default defineConfig({
    entities: [User],
    dbName: process.env.POSTGRES_DB,
    user: process.env.POSTGRES_USER,
    password: process.env.POSTGRES_PASSWORD,
    host: process.env.POSTGRES_HOST,
    port: parseInt(process.env.POSTGRES_PORT || '5432', 10),
    driverOptions: {
        // Optionnel : configs spécifiques au driver
    },
    migrations: {
        path: './migrations', // ou './src/migrations' selon ton projet
        emit: 'ts',
    },
    extensions: [Migrator],
});
