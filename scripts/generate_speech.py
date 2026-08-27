#!/usr/bin/env python3
"""
Generate athena_speech.mp3 with a neural voice (OpenAI TTS, voice "onyx").

Usage:
    export OPENAI_API_KEY=sk-...
    python3 generate_speech.py            # athena_speech.mp3  (HOW ATHENA WORKS)
    python3 generate_speech.py examiner   # athena_examiner.mp3 (THE CHIEF EXAMINER)

- Reads the SPEECH paragraphs directly out of index.html, so the audio always
  matches the on-screen text.
- Uses the same API key as the Chief Examiner backend. Cost: ~US$0.10 per run.
- Standard library only; no pip installs needed.
"""
import json, os, re, sys, urllib.request

VOICE = "onyx"                    # deep male; alternatives: echo, fable, alloy
MODEL = "gpt-4o-mini-tts"         # falls back to tts-1 automatically on 4xx
TONE  = ("Read as a calm, deep-voiced mentor speaking to a teenage student: "
         "warm, unhurried, quietly confident. Pause briefly between paragraphs.")
CHUNK_LIMIT = 3500                # API input limit is 4096 chars per request

TRACKS = {"speech": ("SPEECH", "athena_speech.mp3"),
          "examiner": ("EXAMINER", "athena_examiner.mp3")}

def read_speech(array_name, path="index.html"):
    src = open(path, encoding="utf-8").read()
    m = re.search(r"const %s=\[(.*?)\];" % array_name, src, re.S)
    if not m:
        sys.exit(f"Could not find the {array_name} array in index.html")
    paras = [p.replace('\\"', '"') for p in re.findall(r'"((?:[^"\\\\]|\\\\.)*)"', m.group(1))]
    clean = lambda t: (t.replace("A*", "A star")
                        .replace("\u2014", ", ")
                        .replace("SaveMyExams", "Save My Exams"))
    return [clean(p) for p in paras]

def chunk(paras):
    out, cur = [], ""
    for p in paras:
        if cur and len(cur) + len(p) + 2 > CHUNK_LIMIT:
            out.append(cur); cur = p
        else:
            cur = (cur + "\n\n" + p) if cur else p
    if cur: out.append(cur)
    return out

def tts(text, key, model):
    body = {"model": model, "voice": VOICE, "input": text,
            "response_format": "mp3", "instructions": TONE}
    req = urllib.request.Request(
        "https://api.openai.com/v1/audio/speech",
        data=json.dumps(body).encode(),
        headers={"Authorization": f"Bearer {key}",
                 "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=300) as r:
        return r.read()

def main():
    key = os.environ.get("OPENAI_API_KEY")
    if not key:
        sys.exit("Set OPENAI_API_KEY first (same key the backend uses).")
    track = sys.argv[1] if len(sys.argv) > 1 else "speech"
    if track not in TRACKS:
        sys.exit(f"Usage: generate_speech.py [{'|'.join(TRACKS)}]")
    array_name, outfile = TRACKS[track]
    paras = read_speech(array_name)
    parts = chunk(paras)
    print(f"{len(paras)} paragraphs -> {len(parts)} TTS request(s), voice={VOICE}")
    audio = b""
    for i, part in enumerate(parts, 1):
        print(f"  rendering part {i}/{len(parts)} ({len(part)} chars)...")
        try:
            audio += tts(part, key, MODEL)
        except urllib.error.HTTPError as e:
            if MODEL != "tts-1" and e.code in (400, 404):
                print("  model unavailable, retrying with tts-1...")
                audio += tts(part, key, "tts-1")
            else:
                sys.exit(f"OpenAI TTS failed: {e} {e.read()[:200]}")
    with open(outfile, "wb") as f:
        f.write(audio)
    print(f"Wrote {outfile} ({len(audio)//1024} KB). Commit it next to index.html.")

if __name__ == "__main__":
    main()
