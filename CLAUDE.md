# Project Rules

<project>
  <description>
    Template de projet  Web Full-stack permettant de lancer de générer de nouveaux projets avec les features de bases déjà implémentées.
  </description>
  <packages>
    <package name="back">Back-end implémenté en 
  </packages>
  # FocusFlow – Feature Planning (Spécifications)

FocusFlow est une application de gestion de tâches basée sur la méthode GTD (Getting Things Done).

## Catégories de tâches (GTD)

- **Inbox** : tâches brutes à trier
- **Next Actions** : tâches à réaliser prochainement
- **Projects** : tâches faisant partie de projets à plusieurs étapes
- **Waiting For** : tâches déléguées, en attente d'une action externe
- **Someday/Maybe** : idées ou tâches possibles, un jour
- **Calendar** : tâches liées à des dates précises

_(Pour ce tutoriel, nous simplifierons avec une liste générale)_

## Fonctionnalités Clés

- Ajouter une nouvelle tâche (champ + bouton "Ajouter")
- Lister toutes les tâches
- Marquer une tâche comme terminée (checkbox)
- Modifier le titre d'une tâche
- **UI/UX** : interface minimaliste, responsive (mobile-first)
- **Persistance** : Zustand + localStorage

## Stack Technique

- **Frontend** : React + TypeScript + Tailwind CSS + Zustand
- **Tests** : Playwright MCP via Cursor
- **Backend** : Express.js (optionnel)

    </context>
  </project>
  <ClaudeConfig>
    <Overview>
      <Description>
        This configuration provides guidance to Claude Code (claude.ai/code)
        when working with code in this repository.
      </Description>
      <Project>
        <Type>Fullstack monorepo template</Type>
        <Backends>
          <Implementation>NestJS + Fastify</Implementation>
          <Implementation>Kotlin + Spring Boot</Implementation>
        </Backends>
        <Frontends>
          <Frontend placeholder="true" />
          <Mobile placeholder="true" />
        </Frontends>
      </Project>
    </Overview>

    <RepositoryStructure>
      <Directory name="back" description="NestJS backend with Fastify adapter (primary backend implementation)" />
      <Directory name="back-kotlin" description="Spring Boot + Kotlin backend (alternative implementation)" />
      <Directory name="front" description="Frontend application (empty placeholder)" />
      <Directory name="mobile" description="Mobile application (empty placeholder)" />
    </RepositoryStructure>

    <Backend name="NestJS">
      <TechnologyStack>
        <Framework>NestJS (Fastify adapter)</Framework>
        <ORM>MikroORM with PostgreSQL</ORM>
        <Authentication>JWT (Passport)</Authentication>
        <PackageManager>pnpm</PackageManager>
        <Validation>class-validator, class-transformer</Validation>
      </TechnologyStack>

      <Commands>
        <Category name="Setup">
          <Command>cd back</Command>
          <Command>pnpm install</Command>
        </Category>
        <Category name="Development">
          <Command>pnpm run start:dev</Command>
          <Command>pnpm run start:debug</Command>
        </Category>
        <Category name="BuildAndProduction">
          <Command>pnpm run build</Command>
          <Command>pnpm run start:prod</Command>
        </Category>
        <Category name="Testing">
          <Command>pnpm run test</Command>
          <Command>pnpm run test:watch</Command>
          <Command>pnpm run test:e2e</Command>
          <Command>pnpm run test:cov</Command>
        </Category>
        <Category name="LintingAndFormatting">
          <Command>pnpm run lint</Command>
          <Command>pnpm run format</Command>
        </Category>
        <Category name="Database">
          <Command>pnpm run db:create</Command>
          <Command>pnpm run db:drop</Command>
          <Command>pnpm run db:update</Command>
          <Command>pnpm run db:reset</Command>
          <Command>pnpm run db:generate</Command>
          <Command>pnpm run db:create-migration</Command>
          <Command>pnpm run db:migrate</Command>
          <Command>pnpm run db:rollback</Command>
        </Category>
        <Category name="Docker">
          <Command>docker-compose up -d</Command>
          <Command>docker-compose down</Command>
        </Category>
      </Commands>

      <Architecture>
        <ModuleOrganization>
          <Module name="AppModule" description="Root module that imports MikroORM and AuthModule" />
          <Module name="AuthModule" description="Authentication feature module">
            <Subdir name="controllers" />
            <Subdir name="services" />
            <Subdir name="strategies" />
            <Subdir name="guards" />
            <Subdir name="dto" />
            <Subdir name="interfaces" />
          </Module>
        </ModuleOrganization>

        <DatabaseLayer>
          <Entities path="src/db/entities/" />
          <Configuration file="mikro-orm.config.ts" />
          <Migrations path="migrations/" />
          <Note>MikroORM uses EntityManager (Unit of Work pattern)</Note>
        </DatabaseLayer>

        <AuthenticationFlow>
          <Step>Registration/Login via AuthController</Step>
          <Step>Passwords hashed with bcrypt (10 rounds)</Step>
          <Step>JWT tokens issued (1 day expiration)</Step>
          <Step>JWT strategy validates and attaches user</Step>
          <Step>Protected routes use JwtAuthGuard</Step>
          <Step>Current user via @CurrentUser()</Step>
        </AuthenticationFlow>

        <FastifyAdapter note="Fastify used instead of Express for performance" />
      </Architecture>

      <EnvironmentConfiguration>
        <File name=".env" reference=".env.template" />
        <Variables>
          <Docker>
            <DatabaseContainer>template_db</DatabaseContainer>
            <PgAdminContainer>pgadmin</PgAdminContainer>
            <BackendContainer>template_api</BackendContainer>
          </Docker>
          <PgAdmin>
            <Email>your@email.com</Email>
            <Password>admin</Password>
            <Port>5050</Port>
          </PgAdmin>
          <PostgreSQL>
            <User>postgres</User>
            <Password>motdepasse</Password>
            <Database>wishlist_db</Database>
            <Host>database</Host>
            <Port>5432</Port>
          </PostgreSQL>
          <Application>
            <Port>3000</Port>
          </Application>
          <JWT>
            <Secret>your_jwt_secret_key</Secret>
          </JWT>
        </Variables>
      </EnvironmentConfiguration>

      <DatabaseWorkflow>
        <Step>Create or modify entities</Step>
        <Step>Generate migration (pnpm run db:generate)</Step>
        <Step>Review migration in /migrations</Step>
        <Step>Run migration (pnpm run db:migrate)</Step>
        <Step note="pnpm run db:reset will recreate schema (destructive)" />
      </DatabaseWorkflow>

    </Backend>

    <Backend name="KotlinSpringBoot">
      <TechnologyStack>
        <Framework>Spring Boot 3.5.5</Framework>
        <Language>Kotlin 1.9.25</Language>
        <BuildTool>Maven</BuildTool>
        <JVM>Java 21</JVM>
      </TechnologyStack>

      <Commands>
        <Category name="SetupAndBuild">
          <Command>./mvnw clean install</Command>
          <Command>mvnw.cmd clean install</Command>
        </Category>
        <Category name="Run">
          <Command>./mvnw spring-boot:run</Command>
          <Command>mvnw.cmd spring-boot:run</Command>
        </Category>
        <Category name="Testing">
          <Command>./mvnw test</Command>
          <Command>mvnw.cmd test</Command>
        </Category>
        <Category name="Packaging">
          <Command>./mvnw package</Command>
          <Command>mvnw.cmd package</Command>
        </Category>
      </Commands>

      <Architecture>
        <MainClass>FlorentinB.template.TemplateApplication</MainClass>
        <Package>FlorentinB.template.*</Package>
        <Status>Template/starter only</Status>
      </Architecture>

    </Backend>

    <DevelopmentNotes>
      <CurrentState>
        <Note>NestJS backend is fully functional</Note>
        <Note>Kotlin backend is minimal</Note>
        <Note>Frontend and mobile placeholders</Note>
      </CurrentState>

      <WhenAddingFeatures>
        <Rule>Follow NestJS module structure</Rule>
        <Rule>Use MikroORM migrations for DB changes</Rule>
        <Rule>Protect routes with JwtAuthGuard</Rule>
        <Rule>Validation handled by ValidationPipe</Rule>
      </WhenAddingFeatures>

      <DockerDevelopment>
        <Service name="PostgreSQL" port="5432" />
        <Service name="pgAdmin" port="5050" url="http://localhost:5050" />
        <Service name="Backend" port="3000" />
        <Command>docker-compose up -d</Command>
      </DockerDevelopment>

    </DevelopmentNotes>
  </ClaudeConfig>
