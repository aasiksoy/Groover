# Groover — Swipe-Based Spotify Music Discovery App

Groover is an Android application that lets users discover music through a Tinder-style swipe interface.  
Users swipe right to like a track, swipe left to skip, and Groover automatically builds Spotify playlists from the liked songs.

This project was developed as part of an academic course (Grade: **17/20**) and demonstrates full-stack integration between Android, Spotify Web API, and a cloud-based SQL backend.

---

## Features

- **Spotify OAuth Login** (Authorization Code with PKCE)  
- **Swipe Interface** using CardStackView (Tinder-style interaction)  
- **Real-time Album Art Loading** via Picasso  
- **Cloud Sync** using SQL + REST APIs (Volley)  
- Tracks likes/dislikes + prevents repeat recommendations  
- **Automatic Playlist Creation** on Spotify  
- **Persistent Token Storage** with SharedPreferences  
- Graceful fallback for songs without preview audio  
- Modern UI with smooth animations

---

## Tech Stack

### **Android**
- Java  
- CardStackView  
- Picasso  
- Volley  
- SharedPreferences  

### **Backend**
- MySQL (GroepT API service)  
- RESTful Endpoints  
- JSON Responses  

### **APIs**
- Spotify Web API  
- GroepT SQL Web Services

---

This project uses the following open-source libraries:

CardStackView by yuyakaido
Licensed under the Apache License 2.0
https://github.com/yuyakaido/CardStackView

Picasso by Square
Licensed under the Apache License 2.0
https://github.com/square/picasso

---

### **Contribution**

This is a student project, but improvements, suggestions, and pull requests are welcome!


