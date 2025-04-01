import { Module } from '@nestjs/common';
import { MikroOrmModule } from '@mikro-orm/nestjs';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { AuthModule } from './auth/auth.module';
import mikroOrmConfig from '../mikro-orm.config';

@Module({
    imports: [MikroOrmModule.forRoot(mikroOrmConfig), AuthModule],
    controllers: [AppController],
    providers: [AppService],
})
export class AppModule {}
