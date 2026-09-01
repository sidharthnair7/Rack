# Deploying Rack

The whole thing ships as **one jar**. The React frontend builds into Spring's static resources, so
a single process serves the UI, the API, the storefront, and the uploaded images from one origin —
no second service, no CORS to reconcile, no reverse proxy required.

> **Why this matters for the deadline:** "a judge can click a live URL" is the single biggest lever
> on the *Progress* criterion, and most teams submit a video of localhost. This should take an hour,
> not a day.

---

## 1. Build the jar

```bash
cd frontend && npm install && npm run build && cd ..
./mvnw clean package -DskipTests
```

`npm run build` writes into `src/main/resources/static/`, then Maven packages it in. Output:
`target/rack-0.0.1-SNAPSHOT.jar` (~60MB — it contains the whole frontend).

Verify locally before you ship it:

```bash
java -jar target/rack-0.0.1-SNAPSHOT.jar
```

Open http://localhost:8080. That is byte-for-byte what will run on the server.

> ⚠️ **Do not add `--spring.profiles.active=dev` while `.env` holds the Neon credentials.**
> The dev profile pins `spring.datasource.driver-class-name=org.h2.Driver`, but an environment
> variable outranks a profile file, so `SPRING_DATASOURCE_URL` from `.env` supplies a Postgres URL
> to the H2 driver and the app dies on startup with *"Driver org.h2.Driver claims to not accept
> jdbcUrl"*. Worse, the dev profile also sets `ddl-auto=create-drop`, which against Neon would drop
> your tables on shutdown. With no profile flag the app reads Neon from `.env` with
> `ddl-auto=update`, which is what you actually want both locally and on the server.

---

## 2. Provision the server

An EC2 `t3.micro` is enough and is free-tier eligible. The imaging work all happens on Perfect Corp's servers, so this box mostly just needs to hold the JVM. Add a 1GB swap file. Attach an **Elastic IP** so a reboot does not change the address your DNS record points at.

- **AMI:** Amazon Linux 2023 or Ubuntu 24.04
- **Security group inbound:** 22 (your IP only), 80, 443
- **Note the public IPv4** — this is your `RACK_NAMECOM_STOREFRONT_IP`

```bash
sudo dnf install -y java-25-amazon-corretto-headless   # Amazon Linux
# or: sudo apt update && sudo apt install -y openjdk-25-jre-headless
```

---

## 3. Database

**You already have Neon Postgres configured in `.env`, so there is nothing to install.** Copy the
same `.env` to the server and the app connects to the same database. Skip the rest of this section.

> ⚠️ **The database is shared, the images are not.** Listings live in Neon, but their image files
> live on local disk under `rack.storage.local-dir` (`uploads/`). Neon already holds listings from
> your laptop testing, and the EC2 box will have no `uploads/` directory, so every one of those
> listings renders with a broken image on the deployed storefront. That storefront is where the
> landing page's "See a real listing" button sends a judge.
>
> **So reset the store before you record, then create the demo listings on the deployed box**, so
> rows and image files are written on the same machine. In the Neon SQL editor:
>
> ```sql
> TRUNCATE comps, price_estimates, image_assets, listings, tasks, items, batches RESTART IDENTITY CASCADE;
> ```
>
> This clears listings and their images only. `sellers` and `stores` are left alone, so the demo
> store and `/shop/1` survive. Check the result at `/shop/1`: it should say the closet is empty.

<details>
<summary>Alternative if you would rather not use Neon: H2 file mode</summary>

For a demo this is genuinely fine and removes an entire failure mode:

```bash
java -jar rack.jar \
  --spring.datasource.url=jdbc:h2:file:./rack-db;MODE=PostgreSQL \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.datasource.username=sa \
  --spring.jpa.hibernate.ddl-auto=update
```

Unlike the `dev` profile this persists to disk, so a restart doesn't wipe your demo listings.
You would also need to clear `SPRING_DATASOURCE_*` from `.env`, since those env vars outrank
command-line properties for the same keys.

</details>

---

## 4. Keys

Copy `.env` up next to the jar — `Dotenv` reads it from the working directory, exactly as it does
locally. **Never commit it.**

```bash
scp .env ec2-user@YOUR_IP:~/
scp target/rack-0.0.1-SNAPSHOT.jar ec2-user@YOUR_IP:~/rack.jar
```

Then set the values that are still empty or stale:

```
RACK_NAMECOM_STOREFRONT_IP=<the EC2 public IP>
RACK_PERFECTCORP_MODEL_URL=<public https URL of your synthetic model image>
```

**On `RACK_PERFECTCORP_MODEL_URL`.** Try-on needs a photo of a *person* to dress, and Perfect Corp
fetches it over the network, so it has to be a publicly reachable `https://` URL, not a local
file. Generate the person with Perfect Corp's **AI Image Generator** (text to image, Realistic style, at https://yce.perfectcorp.com/ai-art-generator), then host that image
somewhere public (an S3 object with public read, or Cloudinary, or a GitHub raw URL) and paste the
link here.

⚠️ If you leave it blank the pipeline still runs, but it falls back to Perfect Corp's own stock
sample model, which is a photograph of a real person. Shipping that breaks the project's rule that
no photograph of a real person exists anywhere in the system, it makes that exact sentence in the
Perfect Corp challenge submission false, and it shows Perfect Corp their own sample asset in an
entry to their challenge. The app logs a warning every time it falls back.

⚠️ Keep `RACK_NAMECOM_BASE_URL=https://api.dev.name.com` (sandbox) until the moment you register
the real demo domain. Production registrations charge your card.

---

## 5. Run it on port 80

Java can't bind 80 as a non-root user. Redirect instead of running as root:

```bash
sudo firewall-cmd --permanent --add-forward-port=port=80:proto=tcp:toport=8080
sudo firewall-cmd --reload
# Ubuntu: sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
```

Keep it alive across disconnects and reboots with systemd — `nohup` dies at the worst moment:

```ini
# /etc/systemd/system/rack.service
[Unit]
Description=Rack
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user
ExecStart=/usr/bin/java -jar /home/ec2-user/rack.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now rack
sudo journalctl -u rack -f
```

`WorkingDirectory` matters — it's where `.env`, `uploads/` and `cache/` are read and written.

---

## 6. Point the domain

**Start this at least 48 hours before you record. DNS propagation is the one thing you cannot rush.**

1. Switch `RACK_NAMECOM_BASE_URL` to `https://api.name.com` (production).
2. Register the domain — through the app if you want it on camera, or the name.com dashboard.
3. A record: `@` → your EC2 public IP.
4. Check from outside your machine: `dig +short yourdomain.com`

Then set `RACK_NAMECOM_BASE_URL` back to sandbox so no further live registrations can happen by
accident.

---

## 7. Before you record

- [ ] **Store reset** (the truncate in section 3) run, so no laptop-era listing shows a broken image
- [ ] **Three to five real garment photos run through on the deployed box**, so rows and image
      files are created together. Every previous test used a solid-colour image, so this is also
      the first proof that identification works on actual clothing
- [ ] No product appears twice on `/shop/1`. If it does, you uploaded the same garment twice;
      the duplicate is real data, not a rendering bug
- [ ] `https://yourdomain.com` loads the React app from a clean browser (no cache, not your laptop)
- [ ] Upload → results works on the deployed instance, not just locally
- [ ] A comp link opens a real eBay listing, and no two comps in one panel are the same listing
- [ ] `/shop/{storeId}` renders and a listing page shows the before/after
- [ ] Stripe key valid — a Buy button appears rather than "Contact the seller"
- [ ] `RACK_PERFECTCORP_MODEL_URL` is set to a synthetic model, never a real person, and the
      startup log shows no "running against Perfect Corp's stock sample model" warning
- [ ] SerpApi cache warmed over your exact demo photos, then `rack.serpapi.cache-only=true`
      so takes cost nothing and results are identical every time

**On cache mode:** it replays real recorded responses. Your demo becomes deterministic and free to
re-shoot, while every number on screen still traces to a real listing. Same principle as
pre-propagating DNS — a recorded real result, not a fabricated one.
