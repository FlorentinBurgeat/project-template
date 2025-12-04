# Project Template

A production-ready full-stack template to kickstart your next web application. Built with Vue 3, Kotlin Spring Boot, and PostgreSQL.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 🚀 Features

- ✅ **Complete Authentication System**: JWT-based auth with refresh tokens
- ✅ **SSO Ready**: OpenID Connect support (Google, Facebook)
- ✅ **Account Management**: Registration, login, password/email change, account deletion
- ✅ **Modern Frontend**: Vue 3 + TanStack Query + ShadCN + Tailwind
- ✅ **Robust Backend**: Kotlin + Spring Boot + Exposed ORM
- ✅ **Feature-Based Architecture**: Modular and scalable structure
- ✅ **Docker Ready**: One command to launch the entire stack
- ✅ **Database Migrations**: Flyway for schema versioning
- ✅ **Type-Safe**: End-to-end type safety with Kotlin and TypeScript

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

- **Docker** (v20.10+) and **Docker Compose** (v2.0+)
- **Node.js** (v18+) and **npm** (v9+)
- **Java JDK** (v17+)
- **Maven** (v3.8+)
- **Git**

---

## 🛠️ Installation

### 1. Clone the repository

```bash
git clone https://github.com/your-username/project-template.git
cd project-template
```

### 2. Configure environment variables

Copy the example environment file and configure it:

```bash
cp .env.example .env
```

Edit `.env` and set your values:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=project_template
DB_USERNAME=postgres
DB_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_jwt_secret_key
JWT_ACCESS_EXPIRATION=900000      # 15 minutes in ms
JWT_REFRESH_EXPIRATION=604800000  # 7 days in ms

# SSO (Optional)
SSO_GOOGLE_CLIENT_ID=your_google_client_id
SSO_GOOGLE_CLIENT_SECRET=your_google_client_secret
```

### 3. Launch with Docker

```bash
docker-compose up
```

This will start:
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **PostgreSQL**: localhost:5432

### 4. Access the application

Open your browser and navigate to:
- **Application**: http://localhost:5173
- **API Documentation**: http://localhost:8080/swagger-ui.html (if Swagger is configured)

---

## 📁 Project Structure

```
project-template/
├── backend/                # Kotlin Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── features/      # Feature-based modules
│   │   │   │       └── authentication/
│   │   │   │           ├── controllers/
│   │   │   │           ├── services/
│   │   │   │           ├── models/
│   │   │   │           ├── mappers/
│   │   │   │           └── repositories/
│   │   │   └── resources/
│   │   │       └── db/migration/  # Flyway migrations
│   ├── pom.xml
│   └── CLAUDE.md          # Backend architecture docs
│
├── frontend/              # Vue 3 frontend
│   ├── src/
│   │   ├── components/    # Reusable UI components
│   │   ├── pages/         # Business pages
│   │   ├── api/           # API endpoints & TanStack Query
│   │   ├── model/         # DTOs and mappers
│   │   ├── composables/   # Singleton composables
│   │   └── router/        # Vue Router configuration
│   ├── package.json
│   └── CLAUDE.md          # Frontend architecture docs
│
├── docker-compose.yml
├── .env.example
├── CLAUDE.md              # General project documentation
└── README.md
```

---

## 🎯 Usage

### Development Workflow

#### Adding a New Backend Feature

1. Create a new package under `/backend/src/main/kotlin/features/feature-name`
2. Implement controllers, services, models, mappers, and repositories
3. Add database migration if needed in `/resources/db/migration`
4. Restart the backend service

#### Adding a New Frontend Page

1. Create a new folder in `/frontend/src/pages/page-name`
2. Create the main page component and internal components
3. Add composables for state management
4. Define API endpoints in `/api` if needed
5. Add route in Vue Router configuration

### Running Tests

**Backend:**
```bash
cd backend
./mvnw test
```

**Frontend:**
```bash
cd frontend
npm run test
```

### Building for Production

**Backend:**
```bash
cd backend
./mvnw clean package
```

**Frontend:**
```bash
cd frontend
npm run build
```

---

## 🔒 Security

This template includes:
- **JWT Authentication** with access and refresh tokens
- **Password hashing** (bcrypt)
- **Protected routes** requiring authentication
- **CORS configuration**
- **Environment-based secrets**

**Important:** Always change default secrets in production!

---

## 🌐 SSO Configuration (Optional)

To enable Google or Facebook login:

1. Create OAuth app in [Google Cloud Console](https://console.cloud.google.com/) or [Facebook Developers](https://developers.facebook.com/)
2. Add client ID and secret to `.env`
3. Configure redirect URLs in your OAuth app settings
4. The backend OpenID Connect integration will handle the rest

---

## 🧪 Tech Stack Details

### Frontend
- **Vue 3** - Progressive JavaScript framework
- **Vue Router** - Official routing library
- **TanStack Query** - Powerful data fetching and caching
- **ShadCN** - Beautifully designed components
- **Tailwind CSS** - Utility-first CSS framework
- **Vite** - Next-generation frontend tooling

### Backend
- **Kotlin** - Modern JVM language
- **Spring Boot** - Production-ready framework
- **Exposed** - Kotlin SQL framework
- **Flyway** - Database migration tool
- **Maven** - Dependency management

### Database
- **PostgreSQL** - Robust relational database

---

## 📚 Documentation

- [Backend Architecture](./backend/CLAUDE.md)
- [Frontend Architecture](./frontend/CLAUDE.md)
- [General Documentation](./CLAUDE.md)

---

## 🤝 What This Template IS

✅ A solid starting point for new projects  
✅ A reference architecture with best practices  
✅ A time-saver for initial setup  
✅ Production-ready authentication system  

## 🚫 What This Template IS NOT

❌ A complete application ready to deploy  
❌ A rigid framework you can't modify  
❌ A one-size-fits-all solution  
❌ A replacement for understanding the stack  

**Adapt it to your needs!** This is YOUR starting point.

---

## 🗺️ Roadmap

- [ ] Add email verification flow
- [ ] Add password reset functionality
- [ ] Add role-based access control (RBAC)
- [ ] Add API rate limiting
- [ ] Add logging and monitoring
- [ ] Add CI/CD pipeline examples
- [ ] Add more SSO providers (GitHub, Microsoft)
- [ ] Add example business feature (e.g., todo list)

---

## 🐛 Troubleshooting

### Port already in use
If you get port conflicts, modify ports in `docker-compose.yml`

### Database connection failed
Check your `.env` configuration and ensure PostgreSQL is running

### Frontend can't reach backend
Verify CORS configuration and API URL in frontend config

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Vue.js](https://vuejs.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [ShadCN](https://ui.shadcn.com/)
- [TanStack Query](https://tanstack.com/query)

---

## 📧 Support

If you have questions or need help:
- Open an issue on GitHub
- Check the documentation in `CLAUDE.md` files
- Review the example authentication feature

---

**Happy coding! 🚀**