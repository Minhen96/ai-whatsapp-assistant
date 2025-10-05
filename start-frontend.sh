#!/bin/bash

echo "Starting AI Assistant Frontend..."
echo ""
echo "Make sure you have:"
echo "- Node.js 16+ installed"
echo "- Backend running on port 8080"
echo ""

cd Frontend

echo "Installing dependencies..."
npm install

echo ""
echo "Starting React development server..."
npm start
