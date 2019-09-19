FROM openjdk:8-jdk-alpine AS builder

WORKDIR /app
COPY ./ /app
RUN cd /app
RUN ./gradlew clean build

FROM builder AS runner

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar /app/service.jar

CMD java -Xmx256M -jar /app/service.jar