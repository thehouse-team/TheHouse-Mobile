import json, math, hashlib, random, os

SEED = 42
MODEL_VERSION = "1.0.0"
FEATURE_VERSION = "1.0.0"
FEATURES = ["budget_fit", "services_overlap", "price_affinity", "currency_match"]

random.seed(SEED)

def gen_examples(n=400):
    data = []
    for _ in range(n):
        bf = random.random()
        so = random.random()
        pa = random.random()
        cm = 1.0 if random.random() > 0.15 else 0.0
        z = 1.8*bf + 1.4*so + 1.0*pa + 0.8*cm - 2.1 + random.gauss(0, 0.35)
        p = 1/(1+math.exp(-z))
        y = 1 if random.random() < p else 0
        data.append(([bf, so, pa, cm], y))
    edges = [
        ([1,1,1,1],1),([1,0.8,0.9,1],1),([0.9,0.6,0.8,1],1),
        ([0,0,0,0],0),([0.1,0,0.1,0],0),([0,0.2,0,1],0),
        ([1,0,0.5,1],1),([0.5,0.5,0.5,1],1),([0.2,1,0.2,1],0),([0.8,0.2,0.7,1],1)
    ]
    data.extend(edges)
    return data

def sigmoid(x):
    if x >= 0:
        z = math.exp(-x)
        return 1/(1+z)
    z = math.exp(x)
    return z/(1+z)

def train_logistic(data, lr=0.5, epochs=3000, l2=0.01):
    m = len(FEATURES)
    n = len(data)
    means = [0.0]*m
    stds = [0.0]*m
    for x,_ in data:
        for j in range(m): means[j]+=x[j]
    means = [v/n for v in means]
    for x,_ in data:
        for j in range(m): stds[j]+=(x[j]-means[j])**2
    stds = [math.sqrt(v/n) if v/n > 1e-12 else 1.0 for v in stds]
    def norm(x): return [(x[j]-means[j])/stds[j] for j in range(m)]
    w = [0.0]*m
    b = 0.0
    for _ in range(epochs):
        gw = [0.0]*m
        gb = 0.0
        for x,y in data:
            xn = norm(x)
            z = b + sum(w[j]*xn[j] for j in range(m))
            pred = sigmoid(z)
            err = pred - y
            for j in range(m): gw[j]+=err*xn[j]
            gb += err
        for j in range(m):
            w[j] -= lr*(gw[j]/n + l2*w[j])
        b -= lr*(gb/n)
    return w,b,means,stds

data = gen_examples(400)
w,b,means,stds = train_logistic(data)

artifact = {
    "modelVersion": MODEL_VERSION,
    "featureVersion": FEATURE_VERSION,
    "algorithm": "logistic_regression",
    "features": FEATURES,
    "coefficients": [round(v, 10) for v in w],
    "bias": round(b, 10),
    "scaler": {"mean": [round(v,10) for v in means], "std": [round(v,10) for v in stds]},
    "training": {
        "type": "offline_supervised_synthetic",
        "seed": SEED,
        "examples": len(data),
        "epochs": 3000,
        "learningRate": 0.5,
        "l2": 0.01,
        "note": "Modelo inicial entrenado sobre datos sintéticos controlados; no representa aprendizaje histórico de comportamiento real de usuarios."
    }
}
out_path = os.path.join('app','src','main','assets','smartmatch','model_v1.json')
# hash of content without sha256 field (canonicalized for our purposes: sorted keys not needed, use indent=2)
content_no_hash = json.dumps(artifact, ensure_ascii=False, indent=2, sort_keys=False)
content_hash = hashlib.sha256(content_no_hash.encode('utf-8')).hexdigest()
artifact['sha256'] = content_hash
final = json.dumps(artifact, ensure_ascii=False, indent=2, sort_keys=False)
with open(out_path,'w',encoding='utf-8',newline='\n') as f:
    f.write(final)

train_meta = {
    "modelVersion": MODEL_VERSION,
    "featureVersion": FEATURE_VERSION,
    "seed": SEED,
    "examples": len(data),
    "features": FEATURES,
    "sha256": content_hash,
    "hashMethod": "sha256 of UTF-8 JSON serialization of artifact without sha256 field (json.dumps indent=2, ensure_ascii=False, sort_keys=False, LF)",
    "algorithm": "logistic_regression",
    "note": "Entrenamiento offline determinista sobre dataset sintético controlado, sin PII ni datos productivos."
}
with open(os.path.join('specs','nv-17-smart-match-ml-ai','training','training_metadata.json'),'w',encoding='utf-8',newline='\n') as f:
    json.dump(train_meta, f, ensure_ascii=False, indent=2)
print('OK', content_hash)
