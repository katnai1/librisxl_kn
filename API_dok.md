---
title: Typnormalisering
---

De egenskaperna som påverkas är: verkstyp, instantyp, innehållstyp, issuanceType, carrierType, mediaType, genreFrom


# Verk
####	Nya verkstyper ersätter gamla verkstyper


```json
   "instanceOf": {
        "@type": "Monograph"
                 "Serial"
                 "Collection"
                 "Integrating"
                 
}
```

<details>

<summary>Gamla verktyper</summary>



  ```json


 "instanceOf": {
        "@type": "ManuscriptText"
                 "Text"
                 "Audio"
                 "NotatedMusic"                             
                 "MixedMaterial"
                 "Cartography"
                 "Object"
                 "Multimedia" 
                 "Visual"
                 "Dataset"
                 "Arrangement"
                 "NotatedMovement" 
                 "Software" 
                 "Music"
                 "MusicAudio" 
                 "NonMusicalAudio"
                 "NonMusicAudio"
                 "ManuscriptNotatedMusic" 
                 "Kit" 
                 "ManuscriptCartography" 
                 "MovingImage" 
                 "StillImage" 
                 "ProjectedImage"
}
```


</details>


####	Gamla verkstyperna uttrycks med ContentType eller GenreForm.
<details>

<summary>Nedan hittar ni mappningen mellan gamla verktyperna och contentType (RDA-termlista) eller genreForm (SAOGF-termlista)</summary>
ManuscriptText https://id.kb.se/term/saogf/Handskrifter 
Text https://id.kb.se/term/rda/Text  
Audio https://id.kb.se/term/rda/SpokenWord  
NotatedMusic https://id.kb.se/term/rda/NotatedMusic  
MixedMaterial ny genreForm MixedMaterial, ingen länk i skrivande stund  
Cartography https://id.kb.se/term/rda/CartographicImage  
Object https://id.kb.se/term/rda/ThreeDimensionalForm  
Multimedia https://id.kb.se/term/rda/ComputerProgram  
Visual
Dataset
Arrangement
NotatedMovement https://id.kb.se/term/rda/NotatedMovement
Software https://id.kb.se/term/rda/ComputerProgram
Music
MusicAudio https://id.kb.se/term/rda/PerformedMusic
NonMusicalAudio
NonMusicAudio
ManuscriptNotatedMusic https://id.kb.se/term/saogf/Handskrifter + https://id.kb.se/term/rda/NotatedMusic
Kit ny genreForm Kit, ingen länk i skrivande stund
ManuscriptCartography https://id.kb.se/term/saogf/Handskrifter
MovingImage https://id.kb.se/term/rda/TwoDimensionalMovingImage + https://id.kb.se/term/rda/CartographicImage
StillImage https://id.kb.se/term/rda/StillImage
ProjectedImage
</details>

####	Ny egenskap Kategori (instanceOf/category) på verket. Hit flyttas contentType, genreForm.


