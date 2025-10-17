---
title: Typnormalisering
---
Läs mer om typnormaliseringen
Länk

De egenskaperna som påverkas är: verkstyp, instantyp, innehållstyp, issuanceType, carrierType, mediaType, genreFrom


# Verk
####	Nya verkstyper


```json
   "instanceOf": {
        "@type": "Monograph"
                 "Serial"
                 "Collection"
                 "Integrating"
                 
}
```
####	Ny egenskap Kategori på verket.
```json
   "instanceOf": {
        "category": [
          {
            "@id": "https://id.kb.se/term/rda/Text"
          }
        ],
                 
}

```
####	Gamla verkstyperna uttrycks med contentType (RDA-termlista) eller genreForm (SAOGF-termlista). De flyttas till den nya egenskapen Kategori
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



<details>

<summary>Nedan hittar ni mappningen mellan gamla verktyperna och contentType (RDA-termlista) eller genreForm (SAOGF-termlista)</summary>


ManuscriptText   https://id.kb.se/term/saogf/Handskrifter    
Text  https://id.kb.se/term/rda/Text  
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

# Instans

####	Nya instanstyper


```json
   
        "@type": "Physical"
                 "Digital"
                 
                 
```

####	Ny egenskap Kategori på instasen. Hit flyttas MediaType, CarrierType (och GenreForm om den fanns i instansdelen)
```json
   
        "category": [
        {
          "@id": "https://id.kb.se/term/ktg/PrintedVolume"
        }
      ]
                 


```

####	egenskapen IssuanceType utgår. Uppgifterna finns i nya versktyperna
<details>

<summary>Nedan hittar ni mappningen mellan gamla IssuanceType och versktyperna</summary>  


IssuanceType: Monografisk resurs hämtas från versktyp

```json

"instanceOf": {
        "@type": "Monograph"                
                 
}
```
IssuanceType: Integrerande hämtas från versktyp
```json
"instanceOf": {
        "@type": "Integrating"                
                 
}
```
IssuanceType: Samling hämtas från versktyp
```json
"instanceOf": {
        "@type": "Collection"                
                 
}
```
IssuanceType: Seriell resurs hämtas från versktyp
```json
"instanceOf": {
        "@type": "Serial"                
                 
}
```
</details>





