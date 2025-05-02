from fastapi import FastAPI, HTTPException
from typing import Optional
import mysql.connector

app = FastAPI()

# ✅ MySQL Connection
def get_db_connection():
    try:
        return mysql.connector.connect(
            host="localhost",
            user="Keigan Cardoza",          # Your MySQL username
            password="Messenger_0f.God88",  # Your MySQL password
            database="songs_db"             # Your database
        )
    except mysql.connector.Error as e:
        raise HTTPException(status_code=500, detail=f"Database connection failed: {e}")

# ✅ Home route
@app.get("/")
def home():
    return {"message": "Welcome to the Chords API 🎵"}

# ✅ Get all songs
@app.get("/songs")
def get_songs():
    db = get_db_connection()
    cursor = db.cursor(dictionary=True)

    try:
        query = """
        SELECT 
            id, 
            Artist AS artist, 
            Featured_Artist AS featured_artist, 
            Song_Name AS song_name, 
            Artist_Page AS artist_page, 
            Feat_Artist_Page AS feat_artist_page, 
            Tabs_Page AS tabs_page, 
            Chords_Lyrics AS chords_lyrics
        FROM songs
        """
        
        cursor.execute(query)
        songs = cursor.fetchall()

        if not songs:
            raise HTTPException(status_code=404, detail="No songs found")

        return {"songs": songs}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching songs: {e}")

    finally:
        db.close()

@app.get("/search/")
def search_song(song_name: str = None, artist: str = None):
    db = get_db_connection()
    cursor = db.cursor(dictionary=True)

    try:
        query = """
        SELECT 
            id, 
            Artist AS artist, 
            Featured_Artist AS featured_artist, 
            Song_Name AS song_name, 
            Artist_Page AS artist_page, 
            Feat_Artist_Page AS feat_artist_page, 
            Tabs_Page AS tabs_page, 
            Chords_Lyrics AS chords_lyrics
        FROM songs
        WHERE 1=1
        """
        
        params = []
        
        if song_name:
            query += " AND Song_Name LIKE %s"
            params.append(f"%{song_name}%")

        if artist:
            query += " AND Artist LIKE %s"
            params.append(f"%{artist}%")

        cursor.execute(query, params)
        songs = cursor.fetchall()

        if not songs:
            raise HTTPException(status_code=404, detail="No matching songs found")

        return {"songs": songs}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching songs: {e}")

    finally:
        db.close()

# ✅ Get a song by ID
@app.get("/songs/{song_id}")
def get_song(song_id: int):
    db = get_db_connection()
    cursor = db.cursor(dictionary=True)

    try:
        query = """
        SELECT 
            id, 
            Artist AS artist, 
            Featured_Artist AS featured_artist, 
            Song_Name AS song_name, 
            Artist_Page AS artist_page, 
            Feat_Artist_Page AS feat_artist_page, 
            Tabs_Page AS tabs_page, 
            Chords_Lyrics AS chords_lyrics
        FROM songs
        WHERE id = %s
        """

        cursor.execute(query, (song_id,))
        song = cursor.fetchone()

        if not song:
            raise HTTPException(status_code=404, detail="Song not found")

        return song

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error fetching song: {e}")

    finally:
        db.close()
