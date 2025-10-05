# AI Assistant Frontend

A modern React frontend for the AI Assistant chatbot system with real-time WhatsApp synchronization.

## Features

- 🤖 **AI Chat Interface**: Beautiful chat UI with markdown support
- 📱 **WhatsApp Sync**: Real-time synchronization with WhatsApp messages
- 📚 **Knowledge Storage**: Upload and manage documents
- 🔄 **Real-time Updates**: WebSocket connection for live updates
- 📱 **Responsive Design**: Works on desktop and mobile
- 🎨 **Modern UI**: Clean, intuitive interface

## Quick Start

### Prerequisites

- Node.js 16+
- npm or yarn

### Installation

1. **Install Dependencies**:
   ```bash
   npm install
   ```

2. **Start Development Server**:
   ```bash
   npm start
   ```

3. **Open Browser**:
   Navigate to `http://localhost:3000`

### Build for Production

```bash
npm run build
```

## Project Structure

```
src/
├── components/          # React components
│   ├── ChatInterface.js    # Main chat component
│   ├── MessageList.js        # Message display
│   ├── MessageInput.js       # Input component
│   ├── FileUpload.js         # File upload
│   └── ModeSelector.js       # Mode selection
├── hooks/               # Custom React hooks
│   ├── useWebSocket.js       # WebSocket hook
│   └── useWebSocketSync.js   # Sync hook
├── context/             # React context
│   └── ChatContext.js        # Chat state management
└── App.js               # Main app component
```

## Components

### ChatInterface
Main chat component that handles:
- Message display and input
- File uploads
- Mode switching
- WebSocket synchronization

### MessageList
Displays chat messages with:
- User and bot message styling
- Markdown rendering
- Timestamp display
- Loading states

### FileUpload
Handles file uploads with:
- Drag and drop support
- File type validation
- Progress indicators
- Size limits

### ModeSelector
Initial mode selection:
- Chat with AI
- Store Knowledge
- WhatsApp Sync

## Hooks

### useWebSocket
Custom hook for WebSocket connections:
- Automatic reconnection
- Error handling
- Message sending/receiving

### useWebSocketSync
Specialized hook for chat synchronization:
- WhatsApp message sync
- Frontend message sync
- System notifications

## Context

### ChatContext
Global state management for:
- Messages array
- Loading states
- Connection status
- User sessions

## Styling

The app uses CSS modules for component-specific styling:
- Responsive design
- Modern UI components
- Smooth animations
- Dark/light theme support

## API Integration

The frontend communicates with the backend via:
- REST API (`/api/chat`, `/api/upload`)
- WebSocket (`/ws/chat`)
- File uploads with multipart/form-data

## Development

### Available Scripts

- `npm start` - Start development server
- `npm run build` - Build for production
- `npm test` - Run tests
- `npm run eject` - Eject from Create React App

### Environment Variables

Create `.env` file:
```
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080
```

### Proxy Configuration

The app is configured to proxy API requests to the backend:
```json
{
  "proxy": "http://localhost:8080"
}
```

## Deployment

### Build

```bash
npm run build
```

### Serve

```bash
npx serve -s build
```

### Docker

```dockerfile
FROM node:16-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["npx", "serve", "-s", "build"]
```

## Browser Support

- Chrome 80+
- Firefox 75+
- Safari 13+
- Edge 80+

## Performance

- Lazy loading for components
- Message virtualization for large chats
- Optimized re-renders
- Efficient WebSocket handling

## Security

- Input sanitization
- File type validation
- XSS protection
- CSRF protection

## Troubleshooting

### Common Issues

1. **WebSocket Connection Failed**:
   - Check backend is running
   - Verify CORS settings
   - Check network connectivity

2. **File Upload Issues**:
   - Check file size limits
   - Verify file types
   - Check network connection

3. **Messages Not Syncing**:
   - Check WebSocket connection
   - Verify backend endpoints
   - Check browser console for errors

### Debug Mode

Enable debug logging:
```javascript
localStorage.setItem('debug', 'true');
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

MIT License
