# AI Assistant - WhatsApp Chatbot with React Frontend

A comprehensive AI-powered chatbot system that integrates WhatsApp messaging with a modern React frontend. The system supports knowledge storage, AI chat, and real-time synchronization between WhatsApp and web interface.

## Features

### Backend (Spring Boot)
- **WhatsApp Integration**: Twilio WhatsApp API integration
- **AI Chat**: DeepSeek AI integration for intelligent responses
- **Knowledge Base**: Store and retrieve documents with vector embeddings
- **OCR Support**: Extract text from images and PDFs
- **WebSocket Support**: Real-time communication between frontend and backend
- **File Upload**: Support for various file formats (PDF, DOC, images)

### Frontend (React)
- **Modern UI**: Beautiful, responsive chat interface
- **Real-time Sync**: WebSocket connection for live updates
- **Multiple Modes**: Chat, Knowledge Storage, WhatsApp Sync
- **File Upload**: Drag-and-drop file upload with progress
- **Message History**: Persistent chat history
- **Responsive Design**: Works on desktop and mobile

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   WhatsApp      │    │   React         │    │   Spring Boot   │
│   Users         │◄──►│   Frontend      │◄──►│   Backend       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │                        │
                              │                        │
                              ▼                        ▼
                    ┌─────────────────┐    ┌─────────────────┐
                    │   WebSocket     │    │   PostgreSQL    │
                    │   Connection    │    │   Database      │
                    └─────────────────┘    └─────────────────┘
```

## Prerequisites

- Java 21+
- Node.js 16+
- PostgreSQL
- Twilio Account (for WhatsApp)
- DeepSeek API Key

## Setup Instructions

### 1. Backend Setup

1. **Configure Database**:
   ```sql
   CREATE DATABASE ai_assistant;
   ```

2. **Update Configuration**:
   Edit `src/main/resources/application.properties`:
   ```properties
   # Database
   spring.datasource.url=jdbc:postgresql://localhost:5432/ai_assistant
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   
   # File Storage
   file.storage.path=./docs
   
   # Twilio (for WhatsApp)
   twilio.account.sid=your_twilio_sid
   twilio.auth.token=your_twilio_token
   twilio.from.number=whatsapp:+1234567890
   
   # DeepSeek AI
   deepseek.api.key=your_deepseek_api_key
   ```

3. **Run Backend**:
   ```bash
   ./mvnw spring-boot:run
   ```

### 2. Frontend Setup

1. **Install Dependencies**:
   ```bash
   cd Frontend
   npm install
   ```

2. **Start Development Server**:
   ```bash
   npm start
   ```

3. **Build for Production**:
   ```bash
   npm run build
   ```

### 3. WhatsApp Configuration

1. **Twilio Setup**:
   - Create a Twilio account
   - Set up WhatsApp sandbox or get approval for production
   - Configure webhook URL: `https://your-domain.com/whatsapp/incoming_manual`

2. **Test WhatsApp Integration**:
   - Send a message to your Twilio WhatsApp number
   - Choose option 1 (Store) or 2 (Chat)
   - Test file uploads and AI responses

## Usage

### Frontend Modes

1. **Chat Mode**: Direct AI conversation
   - Ask questions and get intelligent responses
   - Uses stored knowledge for context

2. **Store Mode**: Knowledge management
   - Upload documents (PDF, DOC, images)
   - Store text snippets
   - Build your knowledge base

3. **WhatsApp Sync**: Real-time synchronization
   - Messages sent via WhatsApp appear in frontend
   - Frontend messages can trigger WhatsApp responses
   - Live updates across all connected clients

### API Endpoints

- `POST /api/chat` - Send chat message
- `POST /api/upload` - Upload file
- `GET /api/health` - Health check
- `POST /whatsapp/incoming_manual` - WhatsApp webhook
- `WS /ws/chat` - WebSocket connection

### WebSocket Events

- `whatsapp_message` - WhatsApp message received
- `frontend_message` - Frontend message sent
- `system_message` - System notifications

## Development

### Project Structure

```
├── src/main/java/com/mh/AIAssistant/
│   ├── controller/          # REST controllers
│   ├── service/             # Business logic
│   ├── websocket/           # WebSocket handlers
│   ├── model/               # Data models
│   └── repository/          # Data access
├── Frontend/
│   ├── src/
│   │   ├── components/      # React components
│   │   ├── hooks/           # Custom hooks
│   │   └── context/         # React context
└── docs/                   # Stored files
```

### Key Components

**Backend**:
- `WhatsappController` - Handles WhatsApp webhooks
- `ChatController` - Frontend API endpoints
- `WebSocketService` - Real-time notifications
- `DeepSeekAIService` - AI integration
- `FileStorageService` - File management

**Frontend**:
- `ChatInterface` - Main chat component
- `MessageList` - Message display
- `FileUpload` - File upload component
- `useWebSocketSync` - WebSocket integration

## Deployment

### Backend Deployment

1. **Build JAR**:
   ```bash
   ./mvnw clean package
   ```

2. **Run JAR**:
   ```bash
   java -jar target/AIAssistant-0.0.1-SNAPSHOT.jar
   ```

3. **Docker** (optional):
   ```dockerfile
   FROM openjdk:21-jre-slim
   COPY target/AIAssistant-0.0.1-SNAPSHOT.jar app.jar
   EXPOSE 8080
   CMD ["java", "-jar", "app.jar"]
   ```

### Frontend Deployment

1. **Build**:
   ```bash
   npm run build
   ```

2. **Serve**:
   ```bash
   npx serve -s build
   ```

3. **Nginx Configuration**:
   ```nginx
   server {
       listen 80;
       server_name your-domain.com;
       
       location / {
           root /path/to/build;
           try_files $uri $uri/ /index.html;
       }
       
       location /api {
           proxy_pass http://localhost:8080;
       }
       
       location /ws {
           proxy_pass http://localhost:8080;
           proxy_http_version 1.1;
           proxy_set_header Upgrade $http_upgrade;
           proxy_set_header Connection "upgrade";
       }
   }
   ```

## Troubleshooting

### Common Issues

1. **WebSocket Connection Failed**:
   - Check if backend is running on port 8080
   - Verify CORS settings
   - Check firewall settings

2. **WhatsApp Messages Not Received**:
   - Verify Twilio webhook URL
   - Check Twilio credentials
   - Ensure HTTPS for production

3. **File Upload Issues**:
   - Check file size limits
   - Verify file permissions
   - Check OCR dependencies

4. **Database Connection**:
   - Verify PostgreSQL is running
   - Check connection string
   - Ensure database exists

### Logs

- Backend logs: Check console output
- Frontend logs: Browser developer tools
- WebSocket logs: Network tab in browser

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the logs for error messages
