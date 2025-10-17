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
Gamla verkstyperna uttrycks med contentType (RDA-termlista) eller genreForm (SAOGF-termlista)
####	Ny egenskap Kategori på verket. Hit flyttas gamla verkstyperna och genreForm.
```json
   "instanceOf": {
        "category": [
          {
            "@id": "https://id.kb.se/term/rda/Text"
          }
        ],
                 
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
   
        "@type": "PhysicalResource"
                 "DigitalResource"
                 
                 
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

IssuanceType: _Monografisk resurs_ hämtas från versktyp  




```json

"instanceOf": {
        "@type": "Monograph"                
                 
}
```
IssuanceType: _Integrerande_ hämtas från versktyp
```json
"instanceOf": {
        "@type": "Integrating"                
                 
}
```
IssuanceType: _Samling_ hämtas från versktyp
```json
"instanceOf": {
        "@type": "Collection"                
                 
}
```
IssuanceType: _Seriell resurs_ hämtas från versktyp
```json
"instanceOf": {
        "@type": "Serial"                
                 
}
```
</details>



<details>
<summary>Exempel i JSON efter typnormaliseringen</summary>   
   
```json
{
  "@graph": [
    {
      "@id": "https://id.kb.se/TEMPID",
      "@type": "Record",
      "mainEntity": {
        "@id": "https://id.kb.se/TEMPID#it"
      },
      "descriptionConventions": [
        {
          "@id": "https://id.kb.se/marc/Isbd"
        },
        {
          "@id": "https://id.kb.se/term/enum/Rda"
        }
      ],
      "descriptionLanguage": {
        "@id": "https://id.kb.se/language/swe"
      },
      "encodingLevel": "marc:MinimalLevel",
      "recordStatus": "",
      "marc:catalogingSource": {
        "@id": "https://id.kb.se/marc/CooperativeCatalogingProgram"
      },
      "descriptionCreator": {
        "@id": "https://libris.kb.se/library/SEK"
      }
    },
    {
      "@id": "https://id.kb.se/TEMPID#it",
      "@type": "PhysicalResource",   "OBS! NY INSTANSTYP"
      "hasTitle": [
        {
          "@type": "Title",
          "mainTitle": "",
          "subtitle": ""
        }
      ],
      "responsibilityStatement": "",
      "identifiedBy": [
        {
          "@type": "ISBN",
          "value": "",
          "qualifier": ""
        }
      ],
      "editionStatement": [
        ""
      ],
      "publication": [
        {
          "@type": "PrimaryPublication",
          "year": "",
          "date": "",
          "country": [
            {
              "@id": "https://id.kb.se/country/sw"
            }
          ],
          "place": {
            "@type": "Place",
            "label": [
              ""
            ]
          },
          "agent": {
            "@type": "Agent",
            "label": [
              ""
            ]
          }
        }
      ],
      "manufacture": [
        {
          "@type": "Manufacture",
          "agent": [
            {
              "@type": "Agent",
              "label": [
                ""
              ]
            }
          ],
          "date": "",
          "place": [
            {
              "@type": "Place",
              "label": [
                ""
              ]
            }
          ]
        }
      ],
      "copyright": [
        {
          "@type": "Copyright",
          "date": ""
        }
      ],
      "extent": [
        {
          "@type": "Extent",
          "label": [
            ""
          ]
        }
      ],
      "physicalDetailsNote": "",
      "hasDimensions": {
        "@type": "Dimensions",
        "label": [
          ""
        ]
      },
      "seriesMembership": [
        {
          "@type": "SeriesMembership",
          "inSeries": {
            "@type": "Instance",
            "identifiedBy": [
              {
                "@type": "ISSN",
                "value": ""
              }
            ],
            "instanceOf": {
              "@type": "Work",
              "hasTitle": [
                {
                  "@type": "Title",
                  "mainTitle": ""
                }
              ]
            }
          },
          "marc:seriesTracingPolicy": "",
          "seriesEnumeration": "",
          "seriesStatement": [
            ""
          ]
        }
      ],
      "hasNote": [
        {
          "@type": "Note",
          "label": [
            ""
          ]
        }
      ],
      "instanceOf": {
        "@type": "Monograph",    "OBS! NY VERKSTYP"
        "language": [
          {
            "@id": "https://id.kb.se/language/swe"
          }
        ],
        "category": [
          {
            "@id": "https://id.kb.se/term/rda/Text"     "OBS! NY EGENSKAP MED CONTENTYPE"
          }
        ],
        "classification": [
          {
            "@type": "ClassificationDdc",
            "code": "",
            "edition": "full",
            "editionEnumeration": "23/swe"
          },
          {
            "@type": "Classification",
            "code": "",
            "inScheme": {
              "@type": "ConceptScheme",
              "@id": "https://id.kb.se/term/kssb/8",
              "code": "kssb",
              "version": "8"
            }
          }
        ],
        "contribution": [
          {
            "@type": "PrimaryContribution",
            "agent": null,
            "role": [
              {
                "@id": "https://id.kb.se/relator/author"
              }
            ]
          },
          {
            "@type": "Contribution",
            "agent": null,
            "role": []
          }
        ],
        "hasNote": [
          {
            "@type": "marc:LanguageNote",
            "label": [
              ""
            ]
          }
        ],
        "subject": [],
        "intendedAudience": []
      },
      "contribution": [
        {
          "@type": "Contribution",
          "agent": null,
          "role": []
        }
      ],
      "category": [
        {
          "@id": "https://id.kb.se/term/ktg/PrintedVolume"    "OBS! NY EGENSKAP"
        }
      ]
    }
  ]
}
```
</details>

