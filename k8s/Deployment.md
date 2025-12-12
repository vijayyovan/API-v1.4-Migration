# EMA API v1.4 - Deployment Guide

## Prerequisites

- Docker installed (version 20.10+)
- kubectl configured for target cluster
- Access to container registry
- Database credentials
- Kubernetes cluster access (with appropriate RBAC permissions)

---

## Local Development with Docker

### 1. Build Docker Image

```bash
docker build -t ema-api:v1.4 .
```

### 2. Run with Docker Compose

```bash
# Copy environment template
cp .env.template .env

# Edit .env with your actual credentials
# Use your preferred editor (vim, nano, vscode, etc.)
vim .env

# Start the application
docker-compose up -d

# Check logs
docker-compose logs -f ema-api

# Test endpoint
curl http://localhost:8008/net-ops/ema/hello/v1.4/helloworld
```

### 3. Stop the Application

```bash
docker-compose down
```

---

## Kubernetes Deployment

### 1. Create Namespace

```bash
kubectl create namespace ema
```

### 2. Create Database Secret

**Option A:  Using kubectl (Quick method)**

```bash
kubectl create secret generic ema-db-secret \
  --from-literal=username=YOUR_DB_USERNAME \
  --from-literal=password=YOUR_DB_PASSWORD \
  -n ema
```

**Option B: Using External Secrets Operator (Recommended for production)**

```bash
# Install External Secrets Operator
# https://external-secrets.io/latest/

# Create ExternalSecret resource (example with AWS Secrets Manager)
kubectl apply -f - <<EOF
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: ema-db-secret
  namespace: ema
spec: 
  refreshInterval: 1h
  secretStoreRef:
    name: aws-secrets-manager
    kind: SecretStore
  target:
    name: ema-db-secret
  data:
  - secretKey: username
    remoteRef:
      key: ema/database
      property: username
  - secretKey: password
    remoteRef:
      key: ema/database
      property: password
EOF
```

### 3. Create Image Pull Secret (if using private registry)

```bash
kubectl create secret docker-registry registry-secret \
  --docker-server=your-registry.example.com \
  --docker-username=YOUR_USERNAME \
  --docker-password=YOUR_PASSWORD \
  -n ema
```

### 4. Update ConfigMap with Your Database URL

Edit `k8s/configmap.yaml` and replace: 
- `your-database-host` with your actual database hostname
- `your-service-name` with your Oracle service name

```bash
# Example: 
# database.url: "jdbc:oracle:thin:@//db. example.com:1521/ema_prod"
```

### 5. Deploy Application

```bash
# Apply ConfigMap
kubectl apply -f k8s/configmap.yaml

# Apply Deployment
kubectl apply -f k8s/deployment.yaml

# Apply Service
kubectl apply -f k8s/service.yaml
```

### 6. Verify Deployment

```bash
# Check pod status
kubectl get pods -n ema -w

# Expected output:
# NAME                            READY   STATUS    RESTARTS   AGE
# ema-api-v1-4-xxxxxxxxxx-xxxxx   1/1     Running   0          2m

# Check logs
kubectl logs -f deployment/ema-api-v1-4 -n ema

# Describe pod for detailed info
kubectl describe pod -l app=ema-api -n ema
```

### 7. Test Endpoints (from within cluster)

```bash
# Get pod name
POD_NAME=$(kubectl get pod -n ema -l app=ema-api -o jsonpath='{.items[0].metadata.name}')

# Test hello endpoint
kubectl exec -n ema $POD_NAME -- wget -qO- http://localhost:8008/net-ops/ema/hello/v1.4/helloworld

# Test database connectivity
kubectl exec -n ema $POD_NAME -- wget -qO- http://localhost:8008/net-ops/ema/utils/v1.4/database
```

---

## Available Endpoints

### Health Check Endpoints

| Endpoint | Description | Authentication |
|----------|-------------|----------------|
| `/health/v1.4/liveness` | Liveness probe | None |
| `/health/v1.4/readiness` | Readiness probe | None |
| `/hello/v1.4/helloworld` | Simple health check | None |
| `/utils/v1.4/database` | Database connectivity | Required |

### Business Endpoints

| Endpoint | Description | Authentication |
|----------|-------------|----------------|
| `/outages/v1.4/history/? divisionId={div}&accountNumber={acct}` | Outage history | Required |
| `/outages/v1.4/detail/?divisionId={div}&accountNumber={acct}` | Outage details | Required |
| `/event/v1.4/detail/{eventId}` | Event details by ID | Required |

**Note:** All business endpoints require these headers:
- `Authorization: Basic <token>`
- `session-id: <session>`
- `transaction-id: <transaction>`
- `client-id: <client>`

---

## Configuration

### HikariCP Connection Pool Settings

| Setting | Value | Description |
|---------|-------|-------------|
| Maximum Pool Size | 20 | Max database connections |
| Minimum Idle | 5 | Min idle connections |
| Connection Timeout | 30s | Max wait for connection |
| Idle Timeout | 10min | Max idle time |
| Max Lifetime | 30min | Max connection lifetime |

### Resource Limits

| Resource | Request | Limit |
|----------|---------|-------|
| Memory | 1Gi | 2Gi |
| CPU | 500m | 1000m |

---

## Troubleshooting

### Pod Not Starting

```bash
# Check pod events
kubectl describe pod -l app=ema-api -n ema

# Common issues:
# - ImagePullBackOff:  Check image name and registry credentials
# - CrashLoopBackOff:  Check application logs
# - Pending: Check resource availability
```

### Database Connection Issues

```bash
# Check if secret exists
kubectl get secret ema-db-secret -n ema

# Check if database URL is correct in ConfigMap
kubectl get configmap ema-api-config -n ema -o yaml

# Test database connectivity from pod
kubectl exec -n ema $POD_NAME -- wget -qO- http://localhost:8008/net-ops/ema/utils/v1.4/database
```

### View Application Logs

```bash
# Follow logs
kubectl logs -f deployment/ema-api-v1-4 -n ema

# Get last 100 lines
kubectl logs --tail=100 deployment/ema-api-v1-4 -n ema

# Get logs from previous container (if pod restarted)
kubectl logs --previous deployment/ema-api-v1-4 -n ema
```

### Test Endpoints Directly

```bash
# Port forward to local machine
kubectl port-forward -n ema deployment/ema-api-v1-4 8008:8008

# Then test locally
curl http://localhost:8008/net-ops/ema/hello/v1.4/helloworld
```

---

## Security Best Practices

1. **Never commit secrets to Git**
   - Use `.gitignore` to exclude `.env` files
   - Keep `k8s/secret-template. yaml` as template only

2. **Use external secrets management**
   - External Secrets Operator
   - HashiCorp Vault
   - Cloud provider secrets (AWS/Azure/GCP)

3. **Limit exposed actuator endpoints**
   - Only expose necessary endpoints
   - Consider authentication for actuator endpoints

4. **Use RBAC** for Kubernetes access
   - Limit who can view secrets
   - Audit secret access

5. **Rotate credentials regularly**
   - Database passwords
   - API tokens
   - Registry credentials

---

## Updates and Rollbacks

### Update Deployment

```bash
# Update image tag in deployment. yaml, then: 
kubectl apply -f k8s/deployment.yaml

# Check rollout status
kubectl rollout status deployment/ema-api-v1-4 -n ema
```

### Rollback Deployment

```bash
# Rollback to previous version
kubectl rollout undo deployment/ema-api-v1-4 -n ema

# Rollback to specific revision
kubectl rollout undo deployment/ema-api-v1-4 --to-revision=2 -n ema

# View rollout history
kubectl rollout history deployment/ema-api-v1-4 -n ema
```

---

## Monitoring

### Check Resource Usage

```bash
# CPU and memory usage
kubectl top pod -n ema

# Detailed metrics
kubectl describe pod -l app=ema-api -n ema | grep -A 5 "Limits\|Requests"
```

### Access Actuator Metrics

```bash
# Port forward
kubectl port-forward -n ema deployment/ema-api-v1-4 8008:8008

# View metrics
curl http://localhost:8008/net-ops/ema/actuator/metrics
curl http://localhost:8008/net-ops/ema/actuator/health
```

---

## Support

For issues or questions, contact: 
- DevOps Team:  [your-team@example.com]
- Documentation: [wiki-link]
- Issue Tracker: [github-issues-link]
