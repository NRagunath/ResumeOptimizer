# ResumeOpt AI - ATS-Optimized Resume & Job Portal

**Your all-in-one career assistant platform for resume optimization, job search, and application tracking**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

## 🚀 Features

### 📄 ATS Resume Optimizer
- **AI-Powered Optimization**: Upload your resume and paste a job description to get real-time ATS keyword enhancements
- **Multiple Format Support**: PDF, DOCX, and text input with template preservation
- **Live Side-by-Side Comparison**: See original vs optimized versions with improvement scores
- **Design Templates**: Choose from MINIMAL, PROFESSIONAL, MODERN, and TECH templates
- **Change Tracking**: Visual diff view with detailed change logs

### 💼 Job Discovery & Recommendations
- **11+ Job Portal Integration**: Indeed, LinkedIn, Naukri, Glassdoor, Freshersworld, Internshala, Cutshort, Shine, Wellfound, Jobsora, Hirist
- **Smart Matching**: AI-powered job recommendations with match scores and success probability

- **Real-time Updates**: Live job listings with verified application links

### 📊 Application Tracking & Analytics
- **Comprehensive Dashboard**: Track application status, interview likelihood, and notes
- **Excel Export**: One-click export of job listings and application data
- **Market Insights**: Trending skills, top companies, salary ranges
- **Success Analytics**: Track your application success rate and patterns

### 🤖 AI Integration
- **ChatGPT Integration**: GPT-4o-mini for resume optimization and job matching
- **Perplexity AI**: Enhanced job market insights and recommendations
- **Real-time Processing**: WebSocket-based live updates and notifications

## 🛠️ Technology Stack

- **Backend**: Java 17, Spring Boot 3.3.4, Spring Data JPA
- **Frontend**: Thymeleaf, Bootstrap 5.3.2, Modern CSS/JavaScript
- **Database**: H2 (embedded), with file persistence
- **Web Scraping**: JSoup for HTML parsing and data extraction
- **Document Processing**: Apache PDFBox, Apache POI for PDF/DOCX handling
- **Real-time**: WebSocket with SockJS and STOMP
- **Monitoring**: Spring Actuator with Prometheus metrics
- **Build Tool**: Maven 3.x

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Git

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/resumeopt.git
   cd resumeopt
   ```

2. **Configure API Keys** (Optional but recommended)
   
   Edit `src/main/resources/application.properties`:
   ```properties
   # ChatGPT API (OpenAI)
   chatgpt.api.key=your-openai-api-key
   chatgpt.enabled=true
   
   # Perplexity API
   perplexity.api.key=your-perplexity-api-key
   perplexity.enabled=true
   ```

3. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

4. **Access the Application**
   - Main Application: http://localhost:8080
   - H2 Database Console: http://localhost:8080/h2
   - Health Check: http://localhost:8080/actuator/health
   - Metrics: http://localhost:8080/actuator/prometheus

## 📱 Usage Guide

### Resume Optimization
1. Navigate to `/resume`
2. Upload your resume (PDF/DOCX) or paste text
3. Paste the target job description
4. Select a design template (optional)
5. Click "Optimize Resume" to get:
   - ATS compatibility score improvement
   - Keyword injection suggestions
   - Visual diff of changes
   - Downloadable optimized resume



### Job Recommendations
1. Go to `/jobs` and enter your resume text
2. Get personalized recommendations with:
   - Match scores and success probability
   - Skill gap analysis
   - Improvement suggestions
   - Categorized by suitability

### Application Tracking
1. Access `/tracking` for your dashboard
2. Add applications manually or from job listings
3. Track status, add notes, set reminders
4. Export data to Excel for external analysis

## 🔧 Configuration

### Job Portal Settings
Configure which portals to scrape in `application.properties`:

```properties
# Enable/disable specific portals
job.portals.indeed.enabled=true
job.portals.naukri.enabled=true
job.portals.linkedin.enabled=false  # Disabled due to anti-bot protection

# Scraping parameters
job.portals.dateFilter.maxDaysOld=7
job.portals.experienceLevel=entry_level,fresher,junior
```



### AI Services
Configure AI integration:

```properties
# ChatGPT
chatgpt.model=gpt-4o-mini
chatgpt.enabled=true

# Perplexity
perplexity.model=llama-3.1-sonar-small-128k-online
perplexity.enabled=true
```

## 📊 API Endpoints

### Job Listings
- `GET /jobs/api/all` - Get all jobs with pagination

- `GET /jobs/api/portal/{portalName}` - Get jobs from specific portal
- `POST /jobs/api/refresh` - Trigger fresh scraping
- `GET /jobs/api/search?keyword={term}` - Search jobs by keyword

### Resume Operations
- `POST /resume/optimize` - Optimize resume with job description
- `GET /resume/pdf/{id}` - Download optimized PDF
- `GET /resume/docx/{id}` - Download optimized DOCX
- `GET /resume/changes/{id}` - Get resume changes

### Company Management
- `GET /api/companies/list` - Get all companies
- `POST /api/companies/discover-all` - Discover new companies
- `GET /api/companies/stats` - Get company statistics

## 🏗️ Architecture

```
src/
├── main/
│   ├── java/com/resumeopt/
│   │   ├── controller/          # REST controllers
│   │   ├── service/             # Business logic
│   │   ├── model/               # JPA entities
│   │   ├── repo/                # Data repositories
│   │   ├── config/              # Configuration classes
│   │   ├── realtime/            # WebSocket handlers
│   │   └── scheduler/           # Scheduled tasks
│   └── resources/
│       ├── templates/           # Thymeleaf templates
│       ├── static/              # CSS, JS, assets
│       └── application.properties
└── test/                        # Unit and integration tests
```

## 🔍 Key Components

### Job Scrapers
- **Enhanced Scrapers**: Advanced scraping with retry logic and rate limiting
- **Portal Integration**: 11 major job portals with specialized parsers
- **Company Career Pages**: Direct scraping from company websites
- **Link Verification**: Automatic verification of application URLs

### Resume Processing
- **Multi-format Parser**: PDF, DOCX, and text processing
- **ATS Optimization**: Keyword injection and formatting improvements
- **Template Engine**: Multiple professional resume templates
- **Change Tracking**: Detailed diff generation and change management

### AI Services
- **ChatGPT Service**: Resume optimization and job matching
- **Perplexity Service**: Market insights and trend analysis
- **Real-time Processing**: Asynchronous AI processing with progress updates

## 🚀 Deployment

### Local Development
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production Build
```bash
mvn clean package -Pproduction
java -jar target/resumeopt-0.1.0.jar
```

### Docker (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/resumeopt-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📈 Monitoring & Metrics

- **Health Checks**: `/actuator/health`
- **Prometheus Metrics**: `/actuator/prometheus`
- **Application Info**: `/actuator/info`
- **Database Console**: `/h2` (development only)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/your-username/resumeopt/issues)
- **Documentation**: [Wiki](https://github.com/your-username/resumeopt/wiki)
- **Discussions**: [GitHub Discussions](https://github.com/your-username/resumeopt/discussions)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- OpenAI for ChatGPT API
- Perplexity AI for enhanced insights
- All job portal providers for their data
- Open source community for various libraries used

---

**Made with ❤️ for job seekers and career growth**
