
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY src ./src
COPY public ./public

RUN mkdir -p out dados && \
    javac -encoding UTF-8 -d out \
    src/main/java/br/com/mercadinhofacil/Main.java

EXPOSE 8080

CMD ["sh", "-c", "java -cp out br.com.mercadinhofacil.Main"]
