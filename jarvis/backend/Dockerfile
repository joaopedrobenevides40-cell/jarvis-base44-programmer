FROM denoland/deno:1.41.0

WORKDIR /app
COPY . .

RUN deno cache main.ts

EXPOSE 8000
CMD ["deno", "run", "--allow-net", "--allow-env", "--allow-read", "main.ts"]
