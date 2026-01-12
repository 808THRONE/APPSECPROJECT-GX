# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app

# Accept build arguments
ARG VITE_API_URL=http://localhost/api-gateway/api/v1
ARG VITE_IAM_URL=http://localhost/iam-service/api
ARG VITE_STEGO_URL=http://localhost/stego-module/api
ARG VITE_STEGO_ENABLED=true
ARG VITE_OAUTH_CLIENT_ID=securegate-frontend
ARG VITE_ENV=production

# Set as environment variables for Vite build process
ENV VITE_API_URL=$VITE_API_URL \
    VITE_IAM_URL=$VITE_IAM_URL \
    VITE_STEGO_URL=$VITE_STEGO_URL \
    VITE_STEGO_ENABLED=$VITE_STEGO_ENABLED \
    VITE_OAUTH_CLIENT_ID=$VITE_OAUTH_CLIENT_ID \
    VITE_ENV=$VITE_ENV

COPY package*.json ./
# Use npm ci for production builds (faster and more reliable)
RUN npm ci --production=false
COPY . .
RUN npm run build

# Stage 2: Serve
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
